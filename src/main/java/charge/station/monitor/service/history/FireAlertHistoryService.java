package charge.station.monitor.service.history;

import charge.station.monitor.config.jwt.JwtUtil;
import charge.station.monitor.domain.history.FireAlertHistory;
import charge.station.monitor.domain.history.QFireAlertHistory;
import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.dto.history.*;
import charge.station.monitor.repository.history.FireAlertHistoryRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FireAlertHistoryService {


    private final FireAlertHistoryRepository fireAlertHistoryRepository;
    private final JwtUtil jwtUtil;
    private final JPAQueryFactory queryFactory;


    /**
     * 고장 이력 조회(센터, 충전기, 기간설정, {차량번호})
     */
    public HistoryMainResponseDTO<HistoryReadFireResponseDTO> fireSelect(String accessToken, HistoryMainRequestDTO historyMainRequestDTO,
                                                                 int page, String sortField, String sortDirection) {
        int pageSize = 10; // 한 페이지당 최대 개수
        int currentPage = page <= 0 ? 1 : page; // 페이지 번호가 0 이하이면 1로 설정
        int offset = (currentPage - 1) * pageSize;

        // 1) 센터 ID 목록 생성
        List<Long> centerIds = new ArrayList<>();

        if (historyMainRequestDTO.getCenterId() == null) {
            if (jwtUtil.validateToken(accessToken)) {
                List<String> managedRegions = jwtUtil.getManagedRegions(accessToken);
                centerIds = managedRegions.stream().map(Long::parseLong).collect(Collectors.toList());
            } else {
                throw new CustomException("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED, 401);
            }
        } else {
            centerIds.add(historyMainRequestDTO.getCenterId());
        }

        // 2) 페이징을 포함한 QueryDSL 조회
        Page<FireAlertHistory> fireAlertHistories = findByDynamicConditions(historyMainRequestDTO, centerIds, offset,
                pageSize, sortField, sortDirection);


        // 3) 결과를 DTO로 변환
        List<FireAlertHistory> fireAlertHistoryList = fireAlertHistories.getContent();
        List<HistoryReadFireResponseDTO> items = fireAlertHistoryList.stream()
                .map(f -> new HistoryReadFireResponseDTO(
                        f.getFireAlertHistoryId(),
                        f.getRecordTime(),
                        f.getProcSttus(),
                        f.getType(),
                        f.getCharge().getCenter().getCenterNum() + "-" + f.getCharge().getChargeNum()  // 센터이름(번호)-충전소이름(번호)
                ))
                .toList();

        int total = (int) fireAlertHistories.getTotalElements();
        int totalPages = fireAlertHistories.getTotalPages();

        return new HistoryMainResponseDTO<>(items, total, currentPage, totalPages);

        // 4) 최종 응답
    }


    /**
     * QueryDSL을 이용한 동적 조회
     */
    private Page<FireAlertHistory> findByDynamicConditions(HistoryMainRequestDTO requestDTO, List<Long> centerIds,
                                                           int offset, int pageSize, String sortField, String sortDirection) {
        QFireAlertHistory fireAlertHistory = QFireAlertHistory.fireAlertHistory;
        BooleanBuilder builder = new BooleanBuilder();

        // 1) 센터 목록 IN 조건
        if (centerIds != null && !centerIds.isEmpty()) {
            builder.and(fireAlertHistory.charge.center.centerId.in(centerIds));
        }

//        // 2) 충전기 ID
//        if (requestDTO.getChargeId() != null) {
//            builder.and(fireAlertHistory.charge.chargeId.eq(requestDTO.getChargeId()));
//        }

        // 3) 기간 설정
        if (requestDTO.getStartTime() != null && requestDTO.getEndTime() != null) {
            builder.and(fireAlertHistory.recordTime.between(requestDTO.getStartTime(), requestDTO.getEndTime()));
        } else if (requestDTO.getStartTime() != null) {
            builder.and(fireAlertHistory.recordTime.goe(requestDTO.getStartTime()));
        } else if (requestDTO.getEndTime() != null) {
            builder.and(fireAlertHistory.recordTime.loe(requestDTO.getEndTime()));
        }

        /**
         * 차량번호 조회는 차량 이력 조회시에만.
         */
//        // 4) 차량번호
//        if (requestDTO.getCarNum() != null) {
//            builder.and(fireAlertHistory.charge.carNum.eq(requestDTO.getCarNum()));
//        }

        // 4) 동적 정렬 처리
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(sortField, sortDirection);

        // 5) 전체 개수 조회
        //    - fetchCount()는 deprecated 되었으므로 count() + fetchOne() 사용
        Long total = Optional.ofNullable(
                queryFactory.select(fireAlertHistory.count())
                        .from(fireAlertHistory)
                        .where(builder)
                        .fetchOne()    // 결과가 단 하나(집계)니 fetchOne() 사용
        ).orElse(0L);


        // 6) 실제 페이징 데이터 조회
        List<FireAlertHistory> resultList = queryFactory.selectFrom(fireAlertHistory)
                .where(builder)
                .orderBy(orderSpecifier) // 정렬 적용
                .offset(offset)
                .limit(pageSize)
                .fetch();

        return new PageImpl<>(resultList, PageRequest.of(offset / pageSize, pageSize), total);
    }

    private OrderSpecifier<?> getOrderSpecifier(String sortField, String sortDirection) {
        PathBuilder<FireAlertHistory> pathBuilder = new PathBuilder<>(FireAlertHistory.class, "fireAlertHistory");

        // 동적 필드 선택
        ComparableExpressionBase<?> sortedExpression = pathBuilder.getComparable(sortField, Comparable.class);

        // 정렬 방향 적용
        if ("desc".equalsIgnoreCase(sortDirection)) {
            return new OrderSpecifier<>(Order.DESC, sortedExpression);
        } else {
            return new OrderSpecifier<>(Order.ASC, sortedExpression);
        }
    }


    /**
     * 사후처리 작성 서비스
     */
    @Transactional
    public void fireReasonChk(HistoryCreateFireReasonDTO dto){
        for(Long id : dto.getIds()){
            FireAlertHistory fireAlertHistory = fireAlertHistoryRepository.findById(id)
                    .orElseThrow(() -> new CustomException("이력선택중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST, 400));

            //사후 처리 확인 완료.
            fireAlertHistory.writeProcSttus();
        }
    }




}
