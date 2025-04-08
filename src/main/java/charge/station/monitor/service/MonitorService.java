package charge.station.monitor.service;

import charge.station.monitor.config.jwt.JwtUtil;
import charge.station.monitor.domain.*;
import charge.station.monitor.domain.history.CarHistory;
import charge.station.monitor.domain.history.QCarHistory;
import charge.station.monitor.dto.ChargeMonitorDTO;
import charge.station.monitor.dto.ChargeRuntimeDetailDTO;
import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.repository.ChargeRepository;
import charge.station.monitor.repository.ChargeSttusRepository;
import charge.station.monitor.repository.UserRepository;
import charge.station.monitor.repository.history.CarHistoryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MonitorService {

    private final ChargeRepository chargeRepository;
    private final ChargeSttusRepository chargeSttusRepository;
    private final UserRepository userRepository;
    private final CarHistoryRepository CarHistoryRepository;
    private final JwtUtil jwtUtil;
    private final JPAQueryFactory queryFactory;


    /**
     * 메인 페이지 값 출력
     */
    public List<ChargeMonitorDTO> mainMonitor(String accessToken) {

        //토큰 유효성 검사
        if (!jwtUtil.validateToken(accessToken)) {
            throw new CustomException("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED, 401);
        }

        Long userId = jwtUtil.getUserId(accessToken);
        // 1) 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        // 2) 유저의 regionName 리스트 조회
        QUserRegion qUserRegion = QUserRegion.userRegion;
        QRegion qRegion = QRegion.region;

        List<String> regionNames = queryFactory
                .select(qRegion.regionName)
                .from(qUserRegion)
                .join(qUserRegion.region, qRegion)
                .where(qUserRegion.user.eq(user))
                .fetch();

        // 3) Charge + ChargeSttus + Center 조인하여 DTO로 반환
        QCharge qCharge = QCharge.charge;
        QCenter qCenter = QCenter.center;
        QChargeSttus qChargeSttus = QChargeSttus.chargeSttus;

        List<ChargeMonitorDTO> result = queryFactory
                .select(Projections.constructor(
                        ChargeMonitorDTO.class,
                        qCharge.chargeId,
                        qCharge.chargeNum,
                        qCharge.instlLc,
                        qCharge.companyNm,
                        qCharge.modelNm,
                        qCenter.centerName,
                        qChargeSttus.faultSttus,
                        qChargeSttus.seatSttus
                ))
                .from(qCharge)
                .join(qCharge.center, qCenter)
                .leftJoin(qCharge.chargeSttus, qChargeSttus)
                .where(qCenter.centerName.in(regionNames))
                .fetch();

        return result;
    }


    /**
     * 상세정보 조회
     */
    public ChargeRuntimeDetailDTO getChargeRuntimeDetail(Long chargeId) {

        //토큰 및 권한검증 여기서 할지 말지? 굳이 안해도 되긴할듯.

        QChargeSttus qSttus = QChargeSttus.chargeSttus;
        QCarHistory qHistory = QCarHistory.carHistory;
        QRawDataImg qImg = QRawDataImg.rawDataImg;

        // 1. 상태 정보 조회 (충전 여부, 고장 여부)
        ChargeSttus sttus = queryFactory
                .selectFrom(qSttus)
                .where(qSttus.charge.chargeId.eq(chargeId))
                .fetchOne();

        if (sttus == null) {
            throw new CustomException("충전소 상태 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, 404);
        }

        // 2. 최신 차량 이력 조회 (입차 시간, 차량 번호, 충전 시작 시간)
        CarHistory history = queryFactory
                .selectFrom(qHistory)
                .where(qHistory.charge.chargeId.eq(chargeId))
                .orderBy(qHistory.recordTime.desc())
                .limit(1)
                .fetchOne();

        // 3. 최신 이미지 정보 조회
        RawDataImg img = queryFactory
                .selectFrom(qImg)
                .where(qImg.charge.chargeId.eq(chargeId))
                .orderBy(qImg.recordTime.desc())
                .limit(1)
                .fetchOne();

        return new ChargeRuntimeDetailDTO(
                history != null ? history.getChargeStartTime() : null,
                sttus.getPowerSttus(),
                sttus.getFaultSttus(),
                history != null ? history.getRecordTime() : null,
                img != null ? img.getImgPath() : null,
                history != null ? history.getCarNum() : null
        );
    }

}
