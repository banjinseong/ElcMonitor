package charge.station.monitor.controller;

import charge.station.monitor.dto.ApiResponse;
import charge.station.monitor.dto.ChargeMonitorDTO;
import charge.station.monitor.dto.ChargeRuntimeDetailDTO;
import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.dto.history.*;
import charge.station.monitor.service.MonitorService;
import charge.station.monitor.service.history.CarHistoryService;
import charge.station.monitor.service.history.FaultHistoryService;
import charge.station.monitor.service.history.FireAlertHistoryService;
import charge.station.monitor.service.history.IllegalParkingHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("monitor/*")
public class WebController {


    private final FaultHistoryService faultHistoryService;
    private final IllegalParkingHistoryService illegalParkingHistoryService;
    private final FireAlertHistoryService fireAlertHistoryService;
    private final CarHistoryService carHistoryService;
    private final MonitorService monitorService;


    /**
     * 메인 페이지
     */
    public ResponseEntity<?> mainMonitor(@RequestHeader("Authorization") String authorizationHeader){
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        //메인정보 가져오기
        List<ChargeMonitorDTO> chargeMonitorDTOS = monitorService.mainMonitor(accessToken);

        return ResponseEntity.ok(new ApiResponse<>(200, "메인페이지.", chargeMonitorDTOS));

    }


    /**
     * 세부사항 조회
     */
    @GetMapping("{chargeId}")
    public ResponseEntity<?> mainMonitorDetail(@RequestHeader("Authorization") String authorizationHeader,
                                               @RequestParam Long chargeId){
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        ChargeRuntimeDetailDTO chargeRuntimeDetail = monitorService.getChargeRuntimeDetail(chargeId);


        return ResponseEntity.ok(new ApiResponse<>(200, "상세페이지.", chargeRuntimeDetail));

    }






    /**
     * 고장 등록
     */
    @PostMapping("/create/fault")
    public ResponseEntity<?> createFaultHistory(@RequestBody @Valid HistoryCreateFaultRequestDTO historyCreateFaultRequestDTO){

        faultHistoryService.enroll(historyCreateFaultRequestDTO);
        return ResponseEntity.ok(new ApiResponse<>(200, "고장처리 하셨습니다.", null));
    }


    /**
     * 고장 업데이트(수정, 고장해결)
     */
    @PostMapping("/update/fault")
    public ResponseEntity<?> updateFaultHistory(@RequestBody @Valid HistoryUpdateFaultRequestDTO historyUpdateFaultRequestDTO){


        faultHistoryService.update(historyUpdateFaultRequestDTO);
        return ResponseEntity.ok(new ApiResponse<>(200, "정상처리 하셨습니다.", null));


    }






    /**
     * 고장 이력 조회
     */
    @GetMapping("/history/fault")
    public ResponseEntity<?> selectFaultHistory(@RequestHeader("Authorization") String authorizationHeader,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "recordTime", name = "sortField") String sortField,
                                                @RequestParam(defaultValue = "asc", name = "sortDirection") String sortDirection,
                                                @Valid @RequestBody HistoryMainRequestDTO historyMainRequestDTO) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        HistoryMainResponseDTO<HistoryReadFaultResponseDTO> responseEntity = faultHistoryService.faultSelect(accessToken, historyMainRequestDTO, page, sortField, sortDirection);
        return ResponseEntity.ok(new ApiResponse<>(200, "고장 이력 조회", responseEntity));
    }


    /**
     * 화재위험 이력 조회
     */
    @GetMapping("/history/fire")
    public ResponseEntity<?> selectFireAlert(@RequestHeader("Authorization") String authorizationHeader,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "recordTime", name = "sortField") String sortField,
                                                @RequestParam(defaultValue = "asc", name = "sortDirection") String sortDirection,
                                                @Valid @RequestBody HistoryMainRequestDTO historyMainRequestDTO) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        HistoryMainResponseDTO<HistoryReadFireResponseDTO> responseEntity = fireAlertHistoryService.fireSelect(accessToken, historyMainRequestDTO, page, sortField, sortDirection);
        return ResponseEntity.ok(new ApiResponse<>(200, "화재 이력 조회", responseEntity));

    }


    /**
     * 불법 주정차 이력 조회
     */
    @PostMapping("/history/illegal")
    public ResponseEntity<?> selectIllegalParking(@RequestHeader("Authorization") String authorizationHeader,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "recordTime", name = "sortField") String sortField,
                                                @RequestParam(defaultValue = "asc", name = "sortDirection") String sortDirection,
                                                @Valid @RequestBody HistoryMainRequestDTO historyMainRequestDTO) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        HistoryMainResponseDTO<HistoryReadIllegalResponseDTO> responseEntity = illegalParkingHistoryService.illegalParkingSelect(accessToken, historyMainRequestDTO, page, sortField, sortDirection);
        return ResponseEntity.ok(new ApiResponse<>(200, "불법 주정차 이력 조회", responseEntity));
    }


    /**
     * 차량 주차 이력 조회
     */
    @PostMapping("/history/car")
    public ResponseEntity<?> selectCarHistory(@RequestHeader("Authorization") String authorizationHeader,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "recordTime", name = "sortField") String sortField,
                                                  @RequestParam(defaultValue = "asc", name = "sortDirection") String sortDirection,
                                                  @Valid @RequestBody HistoryMainRequestDTO historyMainRequestDTO) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException("잘못된 토큰 형식입니다.", HttpStatus.BAD_REQUEST, 400);
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        HistoryMainResponseDTO<HistoryReadCarResponseDTO> responseEntity = carHistoryService.carSelect(accessToken, historyMainRequestDTO, page, sortField, sortDirection);
        return ResponseEntity.ok(new ApiResponse<>(200, "자동차 주차 이력 조회", responseEntity));

    }


    /**
     * 화재 사후처리 입력
     */
    @PostMapping("/update/fire")
    public ResponseEntity<?> fireReason(@RequestBody HistoryCreateFireReasonDTO dto) {
        fireAlertHistoryService.fireReasonChk(dto);

        return ResponseEntity.ok(new ApiResponse<>(200, "화재 사루처리 완료", null));
    }







}
