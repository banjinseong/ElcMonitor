package charge.station.monitor.service;

import charge.station.monitor.domain.Charge;
import charge.station.monitor.domain.ChargeSttus;
import charge.station.monitor.domain.RawDataImg;
import charge.station.monitor.domain.RawDataPower;
import charge.station.monitor.domain.history.CarHistory;
import charge.station.monitor.dto.cache.ChargeCacheDTO;
import charge.station.monitor.dto.rawdata.LicensePlateResponseDTO;
import charge.station.monitor.dto.rawdata.RawDataImgRequestDTO;
import charge.station.monitor.dto.rawdata.RawDataPowerRequestDTO;
import charge.station.monitor.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.concurrent.TimeUnit;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RawDataService {

    private final RawDataImgRepository rawDataImgRepository;
    private final RawDataPowerRepository rawDataPowerRepository;
    private final CarHistoryRepository carHistoryRepository;
    private final ChargeRepository chargeRepository;
    private final WebClient webClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChargeSttusRepository chargeSttusRepository;



    /**
     * 이미지 요청값 반환(차량 번호인식)
     * 멀티스레드를 사용하므로 block()을 사용하여 동기적으로 처리 (더 간결함)
     */
    public LicensePlateResponseDTO chkImg(String filePath) {
        // Flask 서버에 전송할 데이터 생성 (JSON 요청)
        Map<String, String> flaskRequest = new HashMap<>();
        flaskRequest.put("image_path", filePath);

        // Flask 서버에 요청을 보내고 응답을 동기적으로 받음
        LicensePlateResponseDTO response = webClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(flaskRequest)
                .retrieve()
                .bodyToMono(LicensePlateResponseDTO.class) // JSON 응답을 DTO로 변환
                .doOnNext(r -> System.out.println("Flask 응답: 차량존재=" + r.isCarExists() + ", 차량번호=" + r.getCarNum()))
                .onErrorResume(error -> {
                    System.err.println("Flask 서버 요청 중 오류 발생: " + error.getMessage());
                    return Mono.empty(); // 에러 발생 시 빈 Mono 반환
                })
                .block(); // 동기적으로 응답을 받을 때까지 대기

        // 응답이 null이면 기본값 반환 (NullPointerException 방지)
        if (response == null) {
            System.out.println("Flask 서버에서 응답을 받지 못했습니다.");
            return new LicensePlateResponseDTO(false, ""); // 기본값 반환
        }

        return response;
    }



    /**
     * 이미지 처리 : 이미지 저장 후 flask(번호판 인식 서버)에 이미지 전송 후 결과값 반환.
     */
    @Transactional
    @Async("ImgData-task")
    public ChargeCacheDTO saveImg(RawDataImgRequestDTO rawDataImgRequestDTO){

        Charge charge = chargeRepository.findById(rawDataImgRequestDTO.getChargeId())
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 충전소 정보입니다 : "+ rawDataImgRequestDTO.getChargeId()));

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

        rawDataImgRepository.save(rawDataImg);

        //이미지속 차량 정보
        LicensePlateResponseDTO licensePlateResponseDTO = chkImg(filePath);

        //이미지속 차량정보 반환(충전소번호, 차량존재, 전력량, 번호판)
        return new ChargeCacheDTO(charge.getChargeId(), licensePlateResponseDTO.isCarExists(), 0, licensePlateResponseDTO.getCarNum());



    }



    @Async("ImgData-task")
    public void manageCarHistory(ChargeCacheDTO nowSttus) {

        String chargeId = nowSttus.getChargeId().toString();

        /**
         * 캐시 데이터를 통한 현재 충전소 차량과 이미지속 차량정보 비교.
         */
        // Redis 캐시 데이터 확인
        Map<Object, Object> cachedSttus = redisTemplate.opsForHash().entries(chargeId);


        if (cachedSttus.isEmpty()) {
            //캐시가 등록 안되어있으면 현재의 정보를 캐시에 업데이트.
            redisTemplate.opsForValue().set(chargeId,nowSttus);
        }else{
            // 기존 캐시 정보 불러오기

            ChargeCacheDTO previousSttus = getCachedData(cachedSttus, chargeId);

            //현재 상태와 캐시데이터 값 비교
            //1. 차량 존재의 값 변경
            if(previousSttus.isCarExists() != nowSttus.isCarExists()) {
                if(nowSttus.isCarExists()){
                    //차량 입차 되었을경우
                    processInCar(nowSttus);
                }else{
                    //차량 출차 되었을경우
                    processOutCar(previousSttus);
                }
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
                        processOutCar(previousSttus);
                        processInCar(nowSttus);
                    }
                }
            }
            // **변경된 데이터만 필드별 업데이트**
            redisTemplate.opsForHash().put(chargeId, "carNum", nowSttus.getCarNum());
            redisTemplate.opsForHash().put(chargeId, "carExists", nowSttus.isCarExists());
        }
    }

    /**
     * 차량 입차 처리
     */
    @Transactional
    public void processInCar(ChargeCacheDTO chargeCacheDTO) {

        //충전소 정보 id로 가져오기.
        Charge charge = chargeRepository.findById(chargeCacheDTO.getChargeId())
                .orElseThrow(() -> new IllegalArgumentException("해당 chargeId가 존재하지 않습니다: " + chargeCacheDTO.getChargeId()));
        CarHistory carHistory = CarHistory.createEntry(chargeCacheDTO.getCarNum(), charge);

        //충전소 현황 가져오기
        ChargeSttus chargeSttus = chargeSttusRepository.findById(chargeCacheDTO.getChargeId())
                .orElseThrow(() -> new IllegalArgumentException("해당 chargeId가 존재하지 않습니다: " + chargeCacheDTO.getChargeId()));

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
                .orElseThrow(() -> new IllegalArgumentException("해당 chargeId가 존재하지 않습니다: " + chargeCacheDTO.getChargeId()));
        CarHistory carHistory = carHistoryRepository.findLatestEntryByCharge(charge)
                .orElseThrow(() -> new IllegalArgumentException("출차할 차량이 없습니다."));


        //충전소 현황 가져오기
        ChargeSttus chargeSttus = chargeSttusRepository.findById(chargeCacheDTO.getChargeId())
                .orElseThrow(() -> new IllegalArgumentException("해당 chargeId가 존재하지 않습니다: " + chargeCacheDTO.getChargeId()));


        //출차처리 하는데 충전완료 기록이 안되어있으면 출차 기점을 충전완료로 기록하기.
        if(carHistory.getChargeEndTime() == null){
            carHistory.completeCharging(LocalDateTime.now());
            chargeSttus.stopCharging();
        }

        chargeSttus.exit(); //현황에서 출차처리.
        carHistory.exit(LocalDateTime.now()); // 출차 시간 업데이트

        /**
         * 해당 영속성 관리 테스트는 필수로 확인 후 삭제할것.
         */
        // 변경 감지 확인용 로그 추가
        System.out.println("출차 시간 업데이트됨: " + carHistory.getOutTime());
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
     */
    public void savePower(RawDataPowerRequestDTO rawDataPowerRequestDTO){

        String chargeId = rawDataPowerRequestDTO.getChargeId().toString();


        Charge charge = chargeRepository.findById(rawDataPowerRequestDTO.getChargeId())
                .orElseThrow(() -> new IllegalArgumentException("해당 chargeId가 존재하지 않습니다: " + rawDataPowerRequestDTO.getChargeId()));

        // Redis 캐시 데이터 확인
        Map<Object, Object> cachedSttus = redisTemplate.opsForHash().entries(chargeId);

        ChargeCacheDTO nowSttus = new ChargeCacheDTO();
        nowSttus.setChargeId(rawDataPowerRequestDTO.getChargeId());
        nowSttus.setPower(rawDataPowerRequestDTO.getPower());

        if (cachedSttus.isEmpty()) {
            //캐시가 등록 안되어있으면 현재의 정보를 캐시에 업데이트.
            redisTemplate.opsForValue().set(chargeId,nowSttus);
        }else{
            // 기존 캐시 정보 불러오기
            ChargeCacheDTO previousSttus = getCachedData(cachedSttus, chargeId);


            //충전소 현황 불러오기(충전중인지 판단 위해서.)
            ChargeSttus chargeSttus = chargeSttusRepository.findById(rawDataPowerRequestDTO.getChargeId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 chargeId가 존재하지 않습니다: " + rawDataPowerRequestDTO.getChargeId()));


            //현재 상태와 캐시데이터 값 비교
            //1. 충전중x + 현재 전력값이 높은경우 -> 충전시작
            if(!chargeSttus.getPowerSttus() && nowSttus.getPower()>=10) {
                chargeSttus.startCharging();


            }
            //2. 충전중 + 현재 전력값이 낮은경우 -> 충전취소
            else if(chargeSttus.getPowerSttus() && nowSttus.getPower()<10){
                chargeSttus.startCharging();
            }

            //db에 데이터에 저장
            RawDataPower rawDataPower = RawDataPower.builder().power(nowSttus.getPower()).charge(charge).build();
            rawDataPowerRepository.save(rawDataPower);

            //캐시에 전력량 저장
            redisTemplate.opsForHash().put(chargeId, "power", nowSttus.getPower());

        }


    }


}
