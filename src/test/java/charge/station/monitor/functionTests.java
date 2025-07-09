package charge.station.monitor;

import charge.station.monitor.domain.Center;
import charge.station.monitor.domain.Charge;
import charge.station.monitor.domain.history.CarHistory;
import charge.station.monitor.domain.history.FaultHistory;
import charge.station.monitor.domain.history.FireAlertHistory;
import charge.station.monitor.domain.history.IllegalParkingHistory;
import charge.station.monitor.dto.history.*;
import charge.station.monitor.dto.user.JoinRequestDTO;
import charge.station.monitor.dto.user.LoginRequestDTO;
import charge.station.monitor.dto.user.UserTokenResponseDTO;
import charge.station.monitor.repository.CenterRepository;
import charge.station.monitor.repository.ChargeRepository;
import charge.station.monitor.repository.history.CarHistoryRepository;
import charge.station.monitor.repository.history.FaultHistoryRepository;
import charge.station.monitor.repository.history.FireAlertHistoryRepository;
import charge.station.monitor.repository.history.IllegalParkingHistoryRepository;
import charge.station.monitor.service.MonitorService;
import charge.station.monitor.service.UserService;
import charge.station.monitor.service.history.CarHistoryService;
import charge.station.monitor.service.history.FaultHistoryService;
import charge.station.monitor.service.history.FireAlertHistoryService;
import charge.station.monitor.service.history.IllegalParkingHistoryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Transactional
@Rollback
public class functionTests {

    @Autowired
    private CarHistoryService carHistoryService;

    @Autowired
    private FaultHistoryService faultHistoryService;

    @Autowired
    private FireAlertHistoryService fireAlertHistoryService;

    @Autowired
    private IllegalParkingHistoryService illegalParkingHistoryService;

    @Autowired
    private UserService userService;

    //------------------------------------------------------------------------------
    //초기 데이터 세팅
    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private ChargeRepository chargeRepository;

    @Autowired
    private CarHistoryRepository carHistoryRepository;

    @Autowired
    private FaultHistoryRepository faultHistoryRepository;

    @Autowired
    private FireAlertHistoryRepository fireAlertHistoryRepository;

    @Autowired
    private IllegalParkingHistoryRepository illegalParkingHistoryRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Center testCenter;
    private Charge testCharge1;
    private Charge testCharge2;

    @BeforeEach
    public void setData() {

        redisTemplate.getConnectionFactory().getConnection().flushDb(); // 전체 삭제

        // 1. Center 생성
        testCenter = centerRepository.save(new Center(null, "서울센터", "서울_1"));

        // 2. Charge 생성 (Center 연결)
        testCharge1 = chargeRepository.save(Charge.builder()
                .chargeNum("CHG-01").instlLc("서울 중구").center(testCenter).build());

        testCharge2 = chargeRepository.save(Charge.builder()
                .chargeNum("CHG-02").instlLc("서울 강남구").center(testCenter).build());

        // 3. CarHistory 11개 생성
        List<CarHistory> carHistories = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            Charge charge = (i <= 6) ? testCharge1 : testCharge2;

            carHistories.add(CarHistory.builder()
                    .carNum("11가" + String.format("%04d", i))
                    .recordTime(LocalDateTime.now().minusDays(2).plusHours(i))
                    .releaseTime(LocalDateTime.now().plusMinutes(i))
                    .charge(charge)
                    .chargeStartTime(LocalDateTime.now().minusMinutes(30))
                    .chargeEndTime(LocalDateTime.now())
                    .build());
        }

        carHistoryRepository.saveAll(carHistories);

        List<FaultHistory> faultHistories = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            Charge charge = (i <= 6) ? testCharge1 : testCharge2;

            faultHistories.add(FaultHistory.builder()
                    .recordTime(LocalDateTime.now().minusDays(2).plusHours(i))
                    .procSttus(i % 2 == 0)
                    .charge(charge)
                    .faultReason("과전류 감지 - " + i)
                    .build());
        }
        faultHistoryRepository.saveAll(faultHistories);

        List<FireAlertHistory> fireAlertHistories = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            Charge charge = (i <= 6) ? testCharge1 : testCharge2;

            fireAlertHistories.add(FireAlertHistory.builder()
                    .recordTime(LocalDateTime.now().minusDays(2).plusHours(i))
                    .charge(charge)
                    .type("고온 감지 " + i)
                    .build());
        }
        fireAlertHistoryRepository.saveAll(fireAlertHistories);

        List<IllegalParkingHistory> illegalParkingHistories = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            Charge charge = (i <= 6) ? testCharge1 : testCharge2;

            illegalParkingHistories.add(IllegalParkingHistory.builder()
                    .carNum("77하" + String.format("%04d", i))
                    .recordTime(LocalDateTime.now().minusDays(2).plusHours(i))
                    .type(i % 2 == 0 ? "점유시간 초과" : "비전기차 진입")
                    .charge(charge)
                    .build());
        }
        illegalParkingHistoryRepository.saveAll(illegalParkingHistories);
    }
    //------------------------------------------------------------------------------


    private JoinRequestDTO createTestUser() {
        JoinRequestDTO dto = new JoinRequestDTO("testuser", "Password1!", "홍길동", "테스트회사", "test@example.com");
        userService.join(dto);
        return dto;
    }

    /**
     * 차량이력 조회 테스트
     */

    @Test
    void 기능_정상_차량이력_페이징조회_테스트() {
        // given
        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadCarResponseDTO> result =
                carHistoryService.carSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(10, result.getItems().size());
        Assertions.assertEquals(2, result.getTotalPages());
    }

    @Test
    void 기능_정상_차량이력_차량번호_페이징조회_테스트() {
        // given
        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, null, null, "11가0001");

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadCarResponseDTO> result =
                carHistoryService.carSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("11가0001", result.getItems().get(0).getCarNum());
    }

    @Test
    void 기능_정상_차량이력_기간조회_테스트() {
        // given
        LocalDateTime start = LocalDateTime.now().minusDays(2).plusHours(3); // day-1 ~ now 사이 = 8개 해당
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, start, end, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadCarResponseDTO> result =
                carHistoryService.carSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(8, result.getItems().size());
        Assertions.assertEquals(1, result.getTotalPages());
    }

    @Test
    void 기능_정상_차량이력_충전소조회_테스트() {
        // given
        HistoryMainRequestDTO request1 = new HistoryMainRequestDTO(
                testCenter.getCenterId(), testCharge1.getChargeId(), null, null, null);

        HistoryMainRequestDTO request2 = new HistoryMainRequestDTO(
                testCenter.getCenterId(), testCharge2.getChargeId(), null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadCarResponseDTO> result1 =
                carHistoryService.carSelect(userTokenResponseDTO.getAccessToken(), request1, 1, "recordTime", "asc");

        HistoryMainResponseDTO<HistoryReadCarResponseDTO> result2 =
                carHistoryService.carSelect(userTokenResponseDTO.getAccessToken(), request2, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(6, result1.getItems().size());
        Assertions.assertEquals(1, result1.getTotalPages());

        Assertions.assertEquals(5, result2.getItems().size());
        Assertions.assertEquals(1, result2.getTotalPages());
    }


    @Test
    void 기능_정상_차량이력_정렬조회_테스트() {
        // given
        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadCarResponseDTO> ascResult =
                carHistoryService.carSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");
        HistoryMainResponseDTO<HistoryReadCarResponseDTO> descResult =
                carHistoryService.carSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "desc");

        // then
        String firstCarAsc = ascResult.getItems().get(0).getCarNum();
        String firstCarDesc = descResult.getItems().get(0).getCarNum();

        Assertions.assertNotEquals(firstCarAsc, firstCarDesc);
    }


    /**
     * 고장이력 조회 테스트
     */

    @Test
    void 기능_정상_고장이력_페이징조회_테스트() {
        // given
        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadFaultResponseDTO> result =
                faultHistoryService.faultSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(10, result.getItems().size());
        Assertions.assertEquals(2, result.getTotalPages());
    }

    @Test
    void 기능_정상_고장이력_기간조회_테스트() {
        // given
        LocalDateTime start = LocalDateTime.now().minusDays(2).plusHours(3); // day-1 ~ now 사이 = 8개 해당
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, start, end, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadFaultResponseDTO> result =
                faultHistoryService.faultSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(8, result.getItems().size());
        Assertions.assertEquals(1, result.getTotalPages());
    }

    @Test
    void 기능_정상_고장이력_충전소조회_테스트() {
        // given
        HistoryMainRequestDTO request1 = new HistoryMainRequestDTO(
                testCenter.getCenterId(), testCharge1.getChargeId(), null, null, null);

        HistoryMainRequestDTO request2 = new HistoryMainRequestDTO(
                testCenter.getCenterId(), testCharge2.getChargeId(), null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadFaultResponseDTO> result1 =
                faultHistoryService.faultSelect(userTokenResponseDTO.getAccessToken(), request1, 1, "recordTime", "asc");

        HistoryMainResponseDTO<HistoryReadFaultResponseDTO> result2 =
                faultHistoryService.faultSelect(userTokenResponseDTO.getAccessToken(), request2, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(6, result1.getItems().size());
        Assertions.assertEquals(1, result1.getTotalPages());

        Assertions.assertEquals(5, result2.getItems().size());
        Assertions.assertEquals(1, result2.getTotalPages());
    }


    @Test
    void 기능_정상_고장이력_정렬조회_테스트() {
        // given
        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadFaultResponseDTO> ascResult =
                faultHistoryService.faultSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");
        HistoryMainResponseDTO<HistoryReadFaultResponseDTO> descResult =
                faultHistoryService.faultSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "desc");

        // then
        Long firstCarAsc = ascResult.getItems().get(0).getFaultHistoryId();
        Long firstCarDesc = descResult.getItems().get(0).getFaultHistoryId();

        Assertions.assertNotEquals(firstCarAsc, firstCarDesc);
    }


    /**
     * 화재이력 조회 테스트
     */

    @Test
    void 기능_정상_화재이력_페이징조회_테스트() {
        // given
        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadFireResponseDTO> result =
                fireAlertHistoryService.fireSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(10, result.getItems().size());
        Assertions.assertEquals(2, result.getTotalPages());
    }

    @Test
    void 기능_정상_화재이력_기간조회_테스트() {
        // given
        LocalDateTime start = LocalDateTime.now().minusDays(2).plusHours(3); // day-1 ~ now 사이 = 8개 해당
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, start, end, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadFireResponseDTO> result =
                fireAlertHistoryService.fireSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(8, result.getItems().size());
        Assertions.assertEquals(1, result.getTotalPages());
    }

    @Test
    void 기능_정상_화재이력_충전소조회_테스트() {
        // given
        HistoryMainRequestDTO request1 = new HistoryMainRequestDTO(
                testCenter.getCenterId(), testCharge1.getChargeId(), null, null, null);

        HistoryMainRequestDTO request2 = new HistoryMainRequestDTO(
                testCenter.getCenterId(), testCharge2.getChargeId(), null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadFireResponseDTO> result1 =
                fireAlertHistoryService.fireSelect(userTokenResponseDTO.getAccessToken(), request1, 1, "recordTime", "asc");

        HistoryMainResponseDTO<HistoryReadFireResponseDTO> result2 =
                fireAlertHistoryService.fireSelect(userTokenResponseDTO.getAccessToken(), request2, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(6, result1.getItems().size());
        Assertions.assertEquals(1, result1.getTotalPages());

        Assertions.assertEquals(5, result2.getItems().size());
        Assertions.assertEquals(1, result2.getTotalPages());
    }


    @Test
    void 기능_정상_화재이력_정렬조회_테스트() {
        // given
        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadFireResponseDTO> ascResult =
                fireAlertHistoryService.fireSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");
        HistoryMainResponseDTO<HistoryReadFireResponseDTO> descResult =
                fireAlertHistoryService.fireSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "desc");

        // then
        Long firstCarAsc = ascResult.getItems().get(0).getFireAlertHistoryId();
        Long firstCarDesc = descResult.getItems().get(0).getFireAlertHistoryId();

        Assertions.assertNotEquals(firstCarAsc, firstCarDesc);
    }


    /**
     * 불법이력 조회 테스트
     */

    @Test
    void 기능_정상_불법이력_페이징조회_테스트() {
        // given
        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadIllegalResponseDTO> result =
                illegalParkingHistoryService.illegalParkingSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(10, result.getItems().size());
        Assertions.assertEquals(2, result.getTotalPages());
    }

    @Test
    void 기능_정상_불법이력_기간조회_테스트() {
        // given
        LocalDateTime start = LocalDateTime.now().minusDays(2).plusHours(3); // day-1 ~ now 사이 = 8개 해당
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, start, end, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadIllegalResponseDTO> result =
                illegalParkingHistoryService.illegalParkingSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(8, result.getItems().size());
        Assertions.assertEquals(1, result.getTotalPages());
    }

    @Test
    void 기능_정상_불법이력_충전소조회_테스트() {
        // given
        HistoryMainRequestDTO request1 = new HistoryMainRequestDTO(
                testCenter.getCenterId(), testCharge1.getChargeId(), null, null, null);

        HistoryMainRequestDTO request2 = new HistoryMainRequestDTO(
                testCenter.getCenterId(), testCharge2.getChargeId(), null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadIllegalResponseDTO> result1 =
                illegalParkingHistoryService.illegalParkingSelect(userTokenResponseDTO.getAccessToken(), request1, 1, "recordTime", "asc");

        HistoryMainResponseDTO<HistoryReadIllegalResponseDTO> result2 =
                illegalParkingHistoryService.illegalParkingSelect(userTokenResponseDTO.getAccessToken(), request2, 1, "recordTime", "asc");

        // then
        Assertions.assertEquals(6, result1.getItems().size());
        Assertions.assertEquals(1, result1.getTotalPages());

        Assertions.assertEquals(5, result2.getItems().size());
        Assertions.assertEquals(1, result2.getTotalPages());
    }


    @Test
    void 기능_정상_불법이력_정렬조회_테스트() {
        // given
        HistoryMainRequestDTO request = new HistoryMainRequestDTO(
                testCenter.getCenterId(), null, null, null, null);

        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        // when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);

        HistoryMainResponseDTO<HistoryReadIllegalResponseDTO> ascResult =
                illegalParkingHistoryService.illegalParkingSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "asc");
        HistoryMainResponseDTO<HistoryReadIllegalResponseDTO> descResult =
                illegalParkingHistoryService.illegalParkingSelect(userTokenResponseDTO.getAccessToken(), request, 1, "recordTime", "desc");

        // then
        Long firstCarAsc = ascResult.getItems().get(0).getIllegalParkingHistoryId();
        Long firstCarDesc = descResult.getItems().get(0).getIllegalParkingHistoryId();

        Assertions.assertNotEquals(firstCarAsc, firstCarDesc);
    }


    /**
     * 등록 테스트
     */
    @Test
    public void 기능_정상_고장이력_등록_테스트(){
// given
        HistoryCreateFaultRequestDTO dto = new HistoryCreateFaultRequestDTO(
                testCharge1.getChargeId(),
                "고장 사유 - 과전류"
        );

        // when
        Long id = faultHistoryService.enroll(dto);

        // then
        List<FaultHistory> all = faultHistoryRepository.findAll();
        Assertions.assertEquals(12, all.size());

    }

    /**
     * 사후처리 테스트
     */
    @Test
    public void 기능_정상_고장사후처리_수정_테스트(){
        // given - 고장 등록
        HistoryCreateFaultRequestDTO createDTO = new HistoryCreateFaultRequestDTO(
                testCharge1.getChargeId(), "최초 고장 사유");

        Long savedFaultId = faultHistoryService.enroll(createDTO);

        // when - 고장 사후처리 수정
        HistoryUpdateFaultRequestDTO updateDTO = new HistoryUpdateFaultRequestDTO(
                savedFaultId, "수정된 고장 사유");

        faultHistoryService.update(updateDTO);

        // then - 수정 내용 확인
        FaultHistory updated = faultHistoryRepository.findById(savedFaultId)
                .orElseThrow(() -> new IllegalStateException("고장 이력이 존재하지 않습니다."));

        Assertions.assertEquals("수정된 고장 사유", updated.getFaultReason());      // 사유 변경 확인
        Assertions.assertTrue(updated.getProcSttus());                             // 처리 상태 true 확인
        Assertions.assertNotNull(updated.getReleaseTime());                        // 해제 시간 설정 확인
    }

    @Test
    public void 기능_정상_불법사후처리_수정_테스트(){
        // given
        List<Long> illegalIds = illegalParkingHistoryRepository.findAll().stream()
                .limit(3) // 3개만 선택
                .map(IllegalParkingHistory::getIllegalParkingHistoryId)
                .toList();

        HistoryUpdateIllegalDTO dto = new HistoryUpdateIllegalDTO(illegalIds);

        // when
        illegalParkingHistoryService.illegalUpdate(dto);

        // then
        for (Long id : illegalIds) {
            IllegalParkingHistory history = illegalParkingHistoryRepository.findById(id).orElseThrow();
            Assertions.assertTrue(history.getProcSttus(), "procSttus가 true여야 합니다.");
        }
    }


    @Test
    public void 기능_정상_화재사후처리_수정_테스트(){
        // given
        List<Long> fireIds = fireAlertHistoryRepository.findAll().stream()
                .limit(3) // 3개만 테스트 대상으로 선정
                .map(FireAlertHistory::getFireAlertHistoryId)
                .toList();

        HistoryUpdateFireDTO dto = new HistoryUpdateFireDTO(fireIds);

        // when
        fireAlertHistoryService.fireUpdate(dto);

        // then
        for (Long id : fireIds) {
            FireAlertHistory history = fireAlertHistoryRepository.findById(id).orElseThrow();
            Assertions.assertTrue(history.getProcSttus(), "procSttus가 true여야 합니다.");
        }
    }
}
