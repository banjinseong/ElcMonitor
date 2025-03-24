package charge.station.monitor.service;

import charge.station.monitor.domain.Charge;
import charge.station.monitor.domain.ChargeSttus;
import charge.station.monitor.domain.RawDataImg;
import charge.station.monitor.domain.RawDataPower;
import charge.station.monitor.domain.history.FireAlertHistory;
import charge.station.monitor.dto.cache.ChargeCacheDTO;
import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.dto.rawdata.LicensePlateResponseDTO;
import charge.station.monitor.dto.rawdata.RawDataImgRequestDTO;
import charge.station.monitor.dto.rawdata.RawDataPowerRequestDTO;
import charge.station.monitor.repository.*;
import charge.station.monitor.repository.history.FireAlertHistoryRepository;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RawDataService {

    private static final int ANOMALY_LIMIT = 3;

    private final RawDataPowerRepository rawDataPowerRepository;
    private final ChargeRepository chargeRepository;
    private final WebClient webClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChargeSttusRepository chargeSttusRepository;
    private final FireAlertHistoryRepository fireAlertHistoryRepository;
    private final RawDataTransaction rawDataTransaction;

    @Resource(name = "ImgData-Task")
    private Executor taskExecutor;


    /**
     * 이미지 요청값 반환(차량 번호인식)
     * 멀티스레드를 사용하므로 block()을 사용하여 동기적으로 처리 (더 간결함)
     */
    public CompletableFuture<LicensePlateResponseDTO> chkImg(String filePath) {
        // Flask 서버에 전송할 데이터 생성 (JSON 요청)
        Map<String, String> flaskRequest = new HashMap<>();
        flaskRequest.put("image_path", filePath);

        // Flask 서버에 요청을 보내고 응답을 동기적으로 받음
        return webClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(flaskRequest)
                .retrieve()
                .bodyToMono(LicensePlateResponseDTO.class) // JSON 응답을 DTO로 변환
                .doOnNext(r -> System.out.println("Flask 응답: 차량존재=" + r.isCarExists() + ", 차량번호=" + r.getCarNum()))
                .onErrorResume(error -> {
                    System.err.println("Flask 서버 요청 중 오류 발생: " + error.getMessage());
                    return Mono.just(new LicensePlateResponseDTO(false, ""));
                })
                .defaultIfEmpty(new LicensePlateResponseDTO(false, "")) // Flask 응답이 `null`일 경우 기본값 설정
                .toFuture(); // 비동기 실행
    }



    /**
     * 이미지 처리 : flask(번호판 인식 서버)에 이미지 전송 후 결과값 반환.
     */
    @Async("ImgData-Task")
    public CompletableFuture<Void> plateForImg(RawDataImgRequestDTO rawDataImgRequestDTO){

        Charge charge = chargeRepository.findById(rawDataImgRequestDTO.getChargeId())
                .orElseThrow(() -> {
                    // ✅ 로그에 남기기
                    log.error("유효하지 않은 충전소 정보입니다 : {}", rawDataImgRequestDTO.getChargeId());
                    return new EntityNotFoundException("유효하지 않은 충전소 정보입니다 : " + rawDataImgRequestDTO.getChargeId());
                });
        LocalDateTime now = LocalDateTime.now();
        // 파일명 포맷팅 (콜론(:) 제거)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String fileName = now.format(formatter) + ".jpg"; // 파일 확장자는 적절히 설정

        //이미지 경로 : ~~/지역/충전소지역/충전소번호
        String imgPath = "C://chargeMonitor/";
        imgPath += charge.getCenter().getCenterName() + "/"
                + charge.getCenter().getCenterNum() + "/"
                + charge.getChargeNum() + "/";

        // 디렉토리 생성 (없으면 생성)
        File dir = new File(imgPath);
        if (!dir.exists()) {
            dir.mkdirs(); // 상위 디렉토리까지 생성
        }

        // 최종 파일 경로
        String filePath = imgPath + fileName;
        File imageFile = new File(filePath);

        try {
            Files.write(Paths.get(filePath), rawDataImgRequestDTO.getImg(), StandardOpenOption.CREATE);
            System.out.println("이미지가 저장되었습니다: " + filePath);
        } catch (IOException e) {
            System.err.println("이미지 저장 실패: " + e.getMessage());
        }

        //객체 생성
        RawDataImg rawDataImg = RawDataImg.builder()
                .imgPath(filePath)
                .charge(charge)
                .build();


        //이미지 db에 저장
        CompletableFuture<Void> saveImgFuture = CompletableFuture.runAsync(() ->
                rawDataTransaction.saveImg(rawDataImg), taskExecutor);
        CompletableFuture<LicensePlateResponseDTO> chkImgFuture = chkImg(filePath);

        // Flask 요청이 완료된 후 차량 이력 관리 실행
        return chkImgFuture.thenAccept(licensePlateResponseDTO -> {
            ChargeCacheDTO chargeCacheDTO = new ChargeCacheDTO(
                    charge.getChargeId(),
                    licensePlateResponseDTO.isCarExists(),
                    0,
                    licensePlateResponseDTO.getCarNum()
            );
            manageCarHistory(chargeCacheDTO);
        });

    }


    /**
     *  차량 이력 관리
     */
    public void manageCarHistory(ChargeCacheDTO nowSttus) {

        String chargeId = nowSttus.getChargeId().toString();

        /**
         * 캐시 데이터를 통한 현재 충전소 차량과 이미지속 차량정보 비교.
         */
        // Redis 캐시 데이터 확인
        Map<Object, Object> cachedSttus = redisTemplate.opsForHash().entries(chargeId);


        if (cachedSttus.isEmpty()) {
            //캐시가 등록 안되어있으면 현재의 정보를 캐시에 업데이트.
            // 캐시 저장 방식 일관성 유지 (Hash 사용)
            redisTemplate.opsForHash().put(chargeId, "carExists", nowSttus.isCarExists());
            redisTemplate.opsForHash().put(chargeId, "carNum", nowSttus.getCarNum());
            if(nowSttus.isCarExists()) {
                rawDataTransaction.processInCar(nowSttus);

            }
        }else{
            // 기존 캐시 정보 불러오기
            ChargeCacheDTO previousSttus = getCachedData(cachedSttus, chargeId);


            //현재 상태와 캐시데이터 값 비교
            //1. 차량 존재의 값 변경
            if(previousSttus.isCarExists() != nowSttus.isCarExists()) {
                if(nowSttus.isCarExists()){
                    //차량 입차 되었을경우
                    rawDataTransaction.processInCar(nowSttus);
                }else{
                    //차량 출차 되었을경우
                    rawDataTransaction.processOutCar(previousSttus);
                }
                redisTemplate.opsForHash().put(chargeId, "carExists", nowSttus.isCarExists());
                redisTemplate.opsForHash().put(chargeId, "carNum", nowSttus.getCarNum());
            }
            // 2.차량존재는 안변했는데 번호판만 바뀌었을 경우
            else if(nowSttus.getCarNum() != null && !previousSttus.getCarNum().equals(nowSttus.getCarNum())){
                //이미지 전송 텀에 다른 차량(번호판 기준)이 들어왔을경우 2회 이상 검증

                String chkNum = chargeId + "chk";
                Object chkCache = redisTemplate.opsForValue().get(chkNum);

                //검사 캐시가 널값인지 확인
                if(chkCache==null){
                    //12분간 2회 이상 찍히는지 확인.(이미지 확인 주기 : 5분)
                    redisTemplate.opsForValue().set(chkNum,1,12, TimeUnit.MINUTES);
                }else{
                    int num = Integer.parseInt(chkCache.toString())+1;
                    if(num>=2){
                        //cache 출차처리, now 입차처리.
                        rawDataTransaction.processOutCar(previousSttus);
                        rawDataTransaction.processInCar(nowSttus);
                        //캐시 삭제
                        redisTemplate.delete(chkNum);
                        redisTemplate.opsForHash().put(chargeId, "carExists", nowSttus.isCarExists());
                        redisTemplate.opsForHash().put(chargeId, "carNum", nowSttus.getCarNum());
                    }
                }
            }


        }
    }



    /**
     * Redis에서 개별 필드로 저장된 데이터를 ChargeCacheDTO로 변환
     */
    public ChargeCacheDTO getCachedData(Map<Object, Object> cachedSttus, String chargeId) {
        ChargeCacheDTO previousSttus = new ChargeCacheDTO();
        previousSttus.setChargeId(Long.parseLong(chargeId));

        // carNum이 null이면 빈 문자열("") 처리
        Object carNumValue = cachedSttus.get("carNum");
        previousSttus.setCarNum(carNumValue != null ? carNumValue.toString() : "");

        // carExists가 null이면 기본값 false 처리
        Object carExistsValue = cachedSttus.get("carExists");
        previousSttus.setCarExists(carExistsValue != null && Boolean.parseBoolean(carExistsValue.toString()));

        // power가 null이면 0.0으로 기본값 설정
        Object powerValue = cachedSttus.get("power");
        previousSttus.setPower(powerValue != null ? Double.parseDouble(powerValue.toString()) : 0.0);

        return previousSttus;
    }



    /**
     * 전력값 처리
     * 전력값을 기준으로 현재 충전소의 충전기가 충전중인지 판별.
     */
    @Transactional
    public void savePower(RawDataPowerRequestDTO rawDataPowerRequestDTO){

        String chargeId = rawDataPowerRequestDTO.getChargeId().toString();
        double currentPower = rawDataPowerRequestDTO.getPower();


        Charge charge = chargeRepository.findById(rawDataPowerRequestDTO.getChargeId())
                .orElseThrow(() -> new CustomException("유효하지 않은 충전소 정보입니다 : " + rawDataPowerRequestDTO.getChargeId(), HttpStatus.NOT_FOUND));
        
        // Redis 캐시 데이터 확인
        Map<Object, Object> cachedSttus = redisTemplate.opsForHash().entries(chargeId);


        // 3. 원시 데이터는 무조건 저장
        RawDataPower rawDataPower = RawDataPower.builder()
                .power(currentPower)
                .charge(charge)
                .build();
        rawDataPowerRepository.save(rawDataPower);

        // 4. 캐시가 비어있으면 전력값 저장 후 종료 (상태 판단은 다음 요청부터)
        if (cachedSttus.isEmpty()) {
            redisTemplate.opsForHash().put(chargeId, "power", currentPower);
        }
        else{


            // 기존 캐시 정보 불러오기
            double previousPower = Double.parseDouble(cachedSttus.get("power").toString());

            // 6. 이상 감지 로직
            //과전력?
            if (currentPower > 40) {
                increaseAnomalyCount("overcurrent", chargeId, charge);
            }

            //50퍼이상 급상승했는가
            if (previousPower > 0) {
                double rate = Math.abs((currentPower - previousPower) / previousPower) * 100;
                if (rate > 50.0) {
                    increaseAnomalyCount("spike", chargeId, charge);
                }
            }

            //10전력량을 기준으로 왔다갔다 하는가?(충전중<>충전x)
            if ((previousPower >= 10 && currentPower < 10) || (previousPower < 10 && currentPower >= 10)) {
                increaseAnomalyCount("fluctuation", chargeId, charge);
            }



            //충전소 현황 불러오기(충전중인지 판단 위해서.)
            ChargeSttus chargeSttus = chargeSttusRepository.findById(rawDataPowerRequestDTO.getChargeId())
                    .orElseThrow(() -> {
                        // ✅ 로그에 남기기
                        log.error("유효하지 않은 충전소 정보입니다 : {}", rawDataPowerRequestDTO.getChargeId());
                        return new EntityNotFoundException("유효하지 않은 충전소 정보입니다 : " + rawDataPowerRequestDTO.getChargeId());
                    });

            //현재 상태와 캐시데이터 값 비교
            //1. 충전중x + 현재 전력값이 높은경우 -> 충전시작
            if(!chargeSttus.getPowerSttus() && currentPower>=10) {
                chargeSttus.startCharging();


            }
            //2. 충전중 + 현재 전력값이 낮은경우 -> 충전취소
            else if(chargeSttus.getPowerSttus() && currentPower<10){
                chargeSttus.startCharging();
            }


            //캐시에 전력량 저장
            redisTemplate.opsForHash().put(chargeId, "power", currentPower);

        }


    }
    public void increaseAnomalyCount(String type, String chargeId, Charge charge) {
        String key = type + ":" + chargeId;

        // Redis에서 해당 키에 대해 숫자 증가 (기존 값 없으면 0 → 1로 초기화됨)
        Long count = redisTemplate.opsForValue().increment(key);

        // 처음 증가된 경우라면 (값이 없어서 새로 1이 됨), TTL 1분 설정
        if (count != null && count == 1L) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }

        // 이상 횟수가 기준을 초과한 경우 → 값을 0으로 재설정 (throw 대신)
        if (count != null && count >= ANOMALY_LIMIT) {
            // 🚨 이상 감지 횟수 초기화
            redisTemplate.delete(key); // 🔥 카운터 자체 제거


            /**
             *  화재 이상감지 db 이력 추가 부분.
             */
            FireAlertHistory fireAlertHistory = FireAlertHistory.builder()
                                                .recordTime(LocalDateTime.now())
                                                .type(type)
                                                .charge(charge)
                                                .build();
            fireAlertHistoryRepository.save(fireAlertHistory);

        }
    }

}
