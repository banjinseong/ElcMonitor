package charge.station.monitor.service;

import charge.station.monitor.domain.Charge;
import charge.station.monitor.domain.ChargeSttus;
import charge.station.monitor.domain.RawDataImg;
import charge.station.monitor.domain.history.CarHistory;
import charge.station.monitor.dto.cache.ChargeCacheDTO;
import charge.station.monitor.repository.history.CarHistoryRepository;
import charge.station.monitor.repository.ChargeRepository;
import charge.station.monitor.repository.ChargeSttusRepository;
import charge.station.monitor.repository.RawDataImgRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RawDataTransaction {

    private final ChargeRepository chargeRepository;
    private final ChargeSttusRepository chargeSttusRepository;
    private final CarHistoryRepository carHistoryRepository;
    private final RawDataImgRepository rawDataImgRepository;


    /**
     * 이미지경로 db에 저장
     */
    @Transactional
    public void saveImg(RawDataImg rawDataImg){
        rawDataImgRepository.save(rawDataImg);
    }



    /**
     * 차량 입차 처리
     */
    @Transactional
    public void processInCar(ChargeCacheDTO chargeCacheDTO) {

        //충전소 정보 id로 가져오기.
        Charge charge = chargeRepository.findById(chargeCacheDTO.getChargeId())
                .orElseThrow(() -> {
                    // ✅ 로그에 남기기
                    log.error("유효하지 않은 충전소 정보입니다 : {}", chargeCacheDTO.getChargeId());
                    return new EntityNotFoundException("유효하지 않은 충전소 정보입니다 : " + chargeCacheDTO.getChargeId());
                });
        CarHistory carHistory = CarHistory.createEntry(chargeCacheDTO.getCarNum(), charge);

        //충전소 현황 가져오기
        ChargeSttus chargeSttus = chargeSttusRepository.findById(chargeCacheDTO.getChargeId())
                .orElseThrow(() -> {
                    // ✅ 로그에 남기기
                    log.error("유효하지 않은 충전소 정보입니다 : {}", chargeCacheDTO.getChargeId());
                    return new EntityNotFoundException("유효하지 않은 충전소 정보입니다 : " + chargeCacheDTO.getChargeId());
                });
        /**
         * 사진 촬영 기점으로 일정 전력 이상을 갖고있으면 바로 충전중이라 판단.
         * 현재는 임의의 값으로 설정해둠.(실제 현장 설치시 나오는 전력값을 바탕으로 수정필요)
         */
        if(chargeCacheDTO.getPower()>=10){
            carHistory.startCharging(LocalDateTime.now());
            //충전소 현황에서 충전중으로 변경.
            chargeSttus.startCharging();
        }

        //현황에서 충전소 자리 유무 변경(자리 차있으면 true)
        chargeSttus.enter();
        carHistoryRepository.save(carHistory);
    }


    /**
     * 차량 출차 처리
     */
    @Transactional
    public void processOutCar(ChargeCacheDTO chargeCacheDTO) {

        //충전소 정보 id로 가져오기.
        Charge charge = chargeRepository.findById(chargeCacheDTO.getChargeId())
                .orElseThrow(() -> {
                    // ✅ 로그에 남기기
                    log.error("유효하지 않은 충전소 정보입니다 : {}", chargeCacheDTO.getChargeId());
                    return new EntityNotFoundException("유효하지 않은 충전소 정보입니다 : " + chargeCacheDTO.getChargeId());
                });
        CarHistory carHistory = carHistoryRepository.findLatestEntryByCharge(charge)
                .orElseThrow(() -> {
                    // ✅ 로그에 남기기
                    log.error("유효하지 않은 충전소 정보입니다 : {}", chargeCacheDTO.getChargeId());
                    return new EntityNotFoundException("유효하지 않은 충전소 정보입니다 : " + chargeCacheDTO.getChargeId());
                });


        //충전소 현황 가져오기
        ChargeSttus chargeSttus = chargeSttusRepository.findById(chargeCacheDTO.getChargeId())
                .orElseThrow(() -> {
                    // ✅ 로그에 남기기
                    log.error("유효하지 않은 충전소 정보입니다 : {}", chargeCacheDTO.getChargeId());
                    return new EntityNotFoundException("유효하지 않은 충전소 정보입니다 : " + chargeCacheDTO.getChargeId());
                });

        //출차처리 하는데 충전완료 기록이 안되어있으면 출차 기점을 충전완료로 기록하기.
        if(carHistory.getChargeStartTime() != null && carHistory.getChargeEndTime() == null){
            carHistory.completeCharging(LocalDateTime.now());
            chargeSttus.stopCharging();
        }

        chargeSttus.exit(); //현황에서 출차처리.
        carHistory.exit(LocalDateTime.now()); // 출차 시간 업데이트

        /**
         * 해당 영속성 관리 테스트는 필수로 확인 후 삭제할것.
         */
        // 변경 감지 확인용 로그 추가
        System.out.println("출차 시간 업데이트됨: " + carHistory.getReleaseTime());
    }
}
