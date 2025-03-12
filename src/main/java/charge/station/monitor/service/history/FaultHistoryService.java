package charge.station.monitor.service.history;

import charge.station.monitor.config.jwt.JwtUtil;
import charge.station.monitor.domain.history.FaultHistory;
import charge.station.monitor.domain.history.QFaultHistory;
import charge.station.monitor.dto.history.HistoryCreateFaultRequestDTO;
import charge.station.monitor.dto.history.HistoryOrderRequestDTO;
import charge.station.monitor.dto.history.HistoryOrderResponseDTO;
import charge.station.monitor.repository.history.FaultHistoryRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityNotFoundException;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FaultHistoryService {

    private final FaultHistoryRepository faultHistoryRepository;
    private final JwtUtil jwtUtil;
    private final JPAQueryFactory queryFactory;


    /**
     * 고장 등록
     */
    public void enroll(HistoryCreateFaultRequestDTO dto){

        FaultHistory faultHistory = new FaultHistory();
        faultHistory.enroll(LocalDateTime.now(), dto.getCharge(), dto.getFaultReason());
        faultHistoryRepository.save(faultHistory);

    }


    /**
     * 고장 사후 처리 완료(수정)
     */
    public void update(Long chargeId){

        FaultHistory history = faultHistoryRepository.findById(chargeId).orElseThrow(() -> {
            // ✅ 로그에 남기기
            log.error("유효하지 않은 충전소 정보입니다 : {}", chargeId);
            return new EntityNotFoundException("유효하지 않은 충전소 정보입니다 : " + chargeId);
        });

        history.update(LocalDateTime.now());

    }





    /**
     * 고장 이력 조회(센터, 충전기, 기간설정, {차량번호})
     */
    public ResponseEntity<?> faultSelect(String accessToken, HistoryOrderRequestDTO historyOrderRequestDTO,
                                         int page, String sortField, String sortDirection) {
        int pageSize = 10; // 한 페이지당 최대 개수
        int currentPage = page <= 0 ? 1 : page; // 페이지 번호가 0 이하이면 1로 설정
        int offset = (currentPage - 1) * pageSize;

        // 1) 센터 ID 목록 생성
        List<Long> centerIds = new ArrayList<>();

        if (historyOrderRequestDTO.getCenterId() == null) {
            if (jwtUtil.validateToken(accessToken)) {
                List<String> managedRegions = jwtUtil.getManagedRegions(accessToken);
                centerIds = managedRegions.stream().map(Long::parseLong).collect(Collectors.toList());
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 만료되었습니다.");
            }
        } else {
            centerIds.add(historyOrderRequestDTO.getCenterId());
        }

        // 2) 페이징을 포함한 QueryDSL 조회
        Page<FaultHistory> faultHistories = findByDynamicConditions(historyOrderRequestDTO, centerIds, offset,
                pageSize, sortField, sortDirection);


        // 3) 결과를 DTO로 변환
        List<FaultHistory> data = faultHistories.getContent();
        int total = (int) faultHistories.getTotalElements();
        int totalPages = faultHistories.getTotalPages();

        HistoryOrderResponseDTO<FaultHistory> responseDTO = new HistoryOrderResponseDTO<>(data, total, currentPage, totalPages);

        // 4) 최종 응답
        return ResponseEntity.ok(responseDTO);
    }


    /**
     * QueryDSL을 이용한 동적 조회
     */
    private Page<FaultHistory> findByDynamicConditions(HistoryOrderRequestDTO requestDTO, List<Long> centerIds,
                                                       int offset, int pageSize, String sortField, String sortDirection) {
        QFaultHistory faultHistory = QFaultHistory.faultHistory;
        BooleanBuilder builder = new BooleanBuilder();

        // 1) 센터 목록 IN 조건
        if (centerIds != null && !centerIds.isEmpty()) {
            builder.and(faultHistory.charge.center.centerId.in(centerIds));
        }

        // 2) 충전기 ID
        if (requestDTO.getChargeId() != null) {
            builder.and(faultHistory.charge.chargeId.eq(requestDTO.getChargeId()));
        }

        // 3) 기간 설정
        if (requestDTO.getStartTime() != null && requestDTO.getEndTime() != null) {
            builder.and(faultHistory.recordTime.between(requestDTO.getStartTime(), requestDTO.getEndTime()));
        } else if (requestDTO.getStartTime() != null) {
            builder.and(faultHistory.recordTime.goe(requestDTO.getStartTime()));
        } else if (requestDTO.getEndTime() != null) {
            builder.and(faultHistory.recordTime.loe(requestDTO.getEndTime()));
        }

        /**
         * 차량번호 조회는 차량 이력 조회시에만.
         */
//        // 4) 차량번호
//        if (requestDTO.getCarNum() != null) {
//            builder.and(faultHistory.charge.carNum.eq(requestDTO.getCarNum()));
//        }

        // 4) 동적 정렬 처리
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(sortField, sortDirection);

        // 5) 전체 개수 조회
        //    - fetchCount()는 deprecated 되었으므로 count() + fetchOne() 사용
        Long total = Optional.ofNullable(
                queryFactory.select(faultHistory.count())
                        .from(faultHistory)
                        .where(builder)
                        .fetchOne()    // 결과가 단 하나(집계)니 fetchOne() 사용
        ).orElse(0L);


        // 6) 실제 페이징 데이터 조회
        List<FaultHistory> resultList = queryFactory.selectFrom(faultHistory)
                .where(builder)
                .orderBy(orderSpecifier) // 정렬 적용
                .offset(offset)
                .limit(pageSize)
                .fetch();

        return new PageImpl<>(resultList, PageRequest.of(offset / pageSize, pageSize), total);
    }

    /**
     * 동적 정렬
     */
    private OrderSpecifier<?> getOrderSpecifier(String sortField, String sortDirection) {
        PathBuilder<FaultHistory> pathBuilder = new PathBuilder<>(FaultHistory.class, "faultHistory");

        // 동적 필드 선택
        ComparableExpressionBase<?> sortedExpression = pathBuilder.getComparable(sortField, Comparable.class);

        // 정렬 방향 적용
        if ("desc".equalsIgnoreCase(sortDirection)) {
            return new OrderSpecifier<>(Order.DESC, sortedExpression);
        } else {
            return new OrderSpecifier<>(Order.ASC, sortedExpression);
        }
    }



    // 추가 메서드 예시: 고장 등록, 삭제, 수정 등...
}


