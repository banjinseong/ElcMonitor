package charge.station.monitor;

import charge.station.monitor.domain.Center;
import charge.station.monitor.domain.Charge;
import charge.station.monitor.domain.RawDataImg;
import charge.station.monitor.domain.history.CarHistory;
import charge.station.monitor.domain.history.FireAlertHistory;
import charge.station.monitor.domain.history.IllegalParkingHistory;
import charge.station.monitor.dto.cache.ChargeCacheDTO;
import charge.station.monitor.dto.rawdata.RawDataPowerRequestDTO;
import charge.station.monitor.repository.CenterRepository;
import charge.station.monitor.repository.ChargeRepository;
import charge.station.monitor.repository.ChargeSttusRepository;
import charge.station.monitor.repository.RawDataImgRepository;
import charge.station.monitor.repository.history.CarHistoryRepository;
import charge.station.monitor.repository.history.FireAlertHistoryRepository;
import charge.station.monitor.repository.history.IllegalParkingHistoryRepository;
import charge.station.monitor.service.RawDataService;
import charge.station.monitor.service.RawDataTransaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@SpringBootTest
@Transactional
@Rollback
public class RawDataTests {

    @Autowired
    private RawDataService rawDataService;

    @Autowired
    private ChargeRepository chargeRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RawDataTransaction rawDataTransaction;

    @Autowired
    private RawDataImgRepository rawDataImgRepository;

    @Autowired
    private IllegalParkingHistoryRepository illegalParkingHistoryRepository;

    @Autowired
    private CarHistoryRepository carHistoryRepository;

    @Autowired
    private CenterRepository centerRepository;

    private Center testCenter;
    private Charge testCharge;
    @Autowired
    private ChargeSttusRepository chargeSttusRepository;
    @Autowired
    private FireAlertHistoryRepository fireAlertHistoryRepository;


    @BeforeEach
    public void setUp() {

        // 1. Center 생성
        testCenter = centerRepository.save(new Center(null, "서울센터", "서울_1"));
        // 테스트용 Charge 등록
        testCharge = Charge.builder()
                .chargeNum("TEST-001")
                .instlLc("서울시 강남구")
                .center(testCenter) // center 테스트가 필요 없다면 null 또는 목객체
                .build();

        chargeRepository.save(testCharge);

        // Redis 캐시 삭제
        redisTemplate.delete(testCharge.getChargeId().toString());
    }

    @Test
    public void 현장_정상_첫입차_테스트() throws Exception {
        // given
        String carNum = "12가3456";
        ChargeCacheDTO entry = new ChargeCacheDTO(
                testCharge.getChargeId(),
                true, // 차량 존재
                15.0, // 전력량 (충전 중으로 간주)
                carNum
        );

        // when
        rawDataService.manageCarHistory(entry);

        // then
        // 입차 이력이 잘 들어갔는지 확인
        List<CarHistory> historyList = carHistoryRepository.findAll();
        Assertions.assertEquals(1, historyList.size());

        CarHistory carHistory = historyList.get(0);
        Assertions.assertEquals(carNum, carHistory.getCarNum());
        Assertions.assertNotNull(carHistory.getChargeStartTime());
        Assertions.assertNull(carHistory.getChargeEndTime());
        Assertions.assertNull(carHistory.getReleaseTime());
    }


    @Test
    public void 현장_정상_출차_테스트() throws Exception{
        // given
        String carNum = "12가3456";
        Long chargeId = testCharge.getChargeId();

        // 🚗 먼저 입차 처리
        ChargeCacheDTO inDTO = new ChargeCacheDTO(
                chargeId,
                true,  // 차량 있음
                15.0,  // 전력 있음 (충전 중)
                carNum
        );
        rawDataService.manageCarHistory(inDTO);

        // 🔁 Redis에 입차 상태로 캐시됨
        // 캐시 확인 (선택적으로 확인해도 됨)
        Map<Object, Object> cached = redisTemplate.opsForHash().entries(chargeId.toString());
        Assertions.assertEquals("true", cached.get("carExists").toString());
        Assertions.assertEquals(carNum, cached.get("carNum"));

        // 📦 출차 처리 DTO
        ChargeCacheDTO outDTO = new ChargeCacheDTO(
                chargeId,
                false,  // 차량 없음 → 출차 상황
                0.0,    // 전력도 없음
                carNum
        );

        // when
        rawDataService.manageCarHistory(outDTO);

        // then
        List<CarHistory> carHistories = carHistoryRepository.findAll();
        Assertions.assertEquals(1, carHistories.size());

        CarHistory carHistory = carHistories.get(0);
        Assertions.assertNotNull(carHistory.getRecordTime());
        Assertions.assertNotNull(carHistory.getReleaseTime(), "출차 시간이 있어야 함");
        Assertions.assertNotNull(carHistory.getChargeEndTime(), "충전 종료 시간이 있어야 함");

    }


    @Test
    public void 현장_정상_다른번호판입차_테스트() throws Exception{
        // given
        String carNum1 = "12가1234"; // 최초 입차 차량
        String carNum2 = "34나5678"; // 변경된 차량 번호

        ChargeCacheDTO first = new ChargeCacheDTO(
                testCharge.getChargeId(),
                true,
                15.0,
                carNum1
        );

        // 최초 입차
        rawDataService.manageCarHistory(first);

        // 첫 번째 변경 (차량 존재 상태는 동일, 번호판만 다름)
        ChargeCacheDTO second = new ChargeCacheDTO(
                testCharge.getChargeId(),
                true,
                15.0,
                carNum2
        );
        rawDataService.manageCarHistory(second); // redis에 count=1 저장됨

        // 캐시가 잘 들어갔는지 확인
        Object firstChkCache = redisTemplate.opsForValue().get(testCharge.getChargeId() + "chk");
        Assertions.assertEquals("1", firstChkCache.toString()); // 1로 세팅되어 있어야 함

        // 두 번째 변경 (같은 차량 번호 다시 인식, 2회 인식)
        rawDataService.manageCarHistory(second); // count>=2 → 출차/입차 트리거

        // when
        List<CarHistory> historyList = carHistoryRepository.findAll();

        // then
        Assertions.assertEquals(2, historyList.size());

        CarHistory firstCar = historyList.get(0);
        CarHistory secondCar = historyList.get(1);

        // 첫 차량은 출차 완료되어야 함
        Assertions.assertEquals(carNum1, firstCar.getCarNum());
        Assertions.assertNotNull(firstCar.getReleaseTime());

        // 두 번째 차량은 입차만 되어 있어야 함
        Assertions.assertEquals(carNum2, secondCar.getCarNum());
        Assertions.assertNull(secondCar.getReleaseTime());
        Assertions.assertNotNull(secondCar.getChargeStartTime());

        // 캐시 초기화 확인
        Object chkNumCache = redisTemplate.opsForValue().get(testCharge.getChargeId() + "chk");
        Assertions.assertNull(chkNumCache); // 2회 처리 후 삭제되었는지 확인
    }


    @Test
    public void 현장_정상_차량존재변화_테스트() throws Exception{
        // given
        String carNum = "12가3456";

        // Step 1: 입차 전 상태 (차량 없음)
        ChargeCacheDTO prev = new ChargeCacheDTO(
                testCharge.getChargeId(),
                false, // 차량 없음
                0.0,
                null
        );

        // Step 2: 입차 상태 감지
        ChargeCacheDTO nowIn = new ChargeCacheDTO(
                testCharge.getChargeId(),
                true, // 차량 존재
                20.0, // 충전 중
                carNum
        );

        // Step 3: 입차 테스트 실행
        rawDataService.manageCarHistory(prev);   // 캐시 초기화용
        rawDataService.manageCarHistory(nowIn);  // 입차 발생

        // then (입차)
        List<CarHistory> historyList = carHistoryRepository.findAll();
        Assertions.assertEquals(1, historyList.size());

        CarHistory carHistory = historyList.get(0);
        Assertions.assertEquals(carNum, carHistory.getCarNum());
        Assertions.assertNotNull(carHistory.getChargeStartTime());
        Assertions.assertNull(carHistory.getReleaseTime());

        // 캐시 검증
        Map<Object, Object> redisMap = redisTemplate.opsForHash().entries(testCharge.getChargeId().toString());
        Assertions.assertEquals("true", redisMap.get("carExists").toString());
        Assertions.assertEquals(carNum, redisMap.get("carNum"));

        // Step 4: 출차 상태 감지
        ChargeCacheDTO nowOut = new ChargeCacheDTO(
                testCharge.getChargeId(),
                false, // 차량 없음
                0.0,
                null
        );
        rawDataService.manageCarHistory(nowOut);

        // then (출차)
        CarHistory updated = carHistoryRepository.findById(carHistory.getCarHistoryId()).orElseThrow();
        Assertions.assertNotNull(updated.getReleaseTime());

        // 캐시 검증
        Map<Object, Object> redisMapAfter = redisTemplate.opsForHash().entries(testCharge.getChargeId().toString());
        Assertions.assertEquals("false", redisMapAfter.get("carExists").toString());
        Assertions.assertNull(redisMapAfter.get("carNum"));

    }



    @Test
    public void 현장_정상_불법주정차기록_테스트() throws Exception{
        // given
        String carNum = "33가4444";

        // 1. 오래 전 입차 처리 (15시간 전으로 설정)
        LocalDateTime fifteenHoursAgo = LocalDateTime.now().minusHours(15);

        CarHistory carHistory = CarHistory.builder()
                .carNum(carNum)
                .recordTime(fifteenHoursAgo)
                .charge(testCharge)
                .build();
        carHistoryRepository.save(carHistory);

        // 캐시 정보 업데이트
        redisTemplate.opsForHash().put(testCharge.getChargeId().toString(), "carExists", true);
        redisTemplate.opsForHash().put(testCharge.getChargeId().toString(), "carNum", carNum);

        // 출차 정보 DTO
        ChargeCacheDTO outDto = new ChargeCacheDTO(
                testCharge.getChargeId(),
                false,  // 차량 없음
                0.0,
                carNum
        );

        // when
        rawDataService.manageCarHistory(outDto);

        // then
        List<IllegalParkingHistory> result = illegalParkingHistoryRepository.findAll();
        Assertions.assertEquals(1, result.size());

        IllegalParkingHistory history = result.get(0);
        Assertions.assertEquals("점유시간 초과", history.getType());
        Assertions.assertEquals(carNum, history.getCarNum());
        Assertions.assertEquals(testCharge.getChargeId(), history.getCharge().getChargeId());

    }



    @Test
    public void 현장_정상_충전상태변화_테스트() {
        // 1. 전력값 낮음 → 충전 시작 안함
        rawDataService.savePower(new RawDataPowerRequestDTO(5.0, testCharge.getChargeId()));
        Assertions.assertFalse(chargeSttusRepository.findById(testCharge.getChargeId()).get().getPowerSttus());

        // 2. 전력값 상승 → 충전 시작
        rawDataService.savePower(new RawDataPowerRequestDTO(15.0, testCharge.getChargeId()));
        Assertions.assertTrue(chargeSttusRepository.findById(testCharge.getChargeId()).get().getPowerSttus());

        // 3. 전력값 하락 → 충전 종료
        rawDataService.savePower(new RawDataPowerRequestDTO(3.0, testCharge.getChargeId()));
        Assertions.assertFalse(chargeSttusRepository.findById(testCharge.getChargeId()).get().getPowerSttus());
    }

    @Test
    public void 현장_정상_화재감지이력_테스트() {
        // 과전력 연속 감지로 화재 기록 생성 테스트 (기준 3회 이상-첫 기록은 패스)
        for (int i = 0; i < 4; i++) {
            rawDataService.savePower(new RawDataPowerRequestDTO(45.0, testCharge.getChargeId()));
        }

        List<FireAlertHistory> fireList = fireAlertHistoryRepository.findAll();
        Assertions.assertEquals(1, fireList.size());

        FireAlertHistory fire = fireList.get(0);
        Assertions.assertEquals("overcurrent", fire.getType());
        Assertions.assertEquals(testCharge.getChargeId(), fire.getCharge().getChargeId());
    }
}