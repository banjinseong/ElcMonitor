package charge.station.monitor.controller;

import charge.station.monitor.dto.history.HistoryCreateFaultRequestDTO;
import charge.station.monitor.dto.history.HistoryOrderRequestDTO;
import charge.station.monitor.dto.history.HistoryReasonDTO;
import charge.station.monitor.service.history.CarHistoryService;
import charge.station.monitor.service.history.FaultHistoryService;
import charge.station.monitor.service.history.FireAlertHistoryService;
import charge.station.monitor.service.history.IllegalParkingHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("monitor/*")
public class WebController {


    private final FaultHistoryService faultHistoryService;
    private final IllegalParkingHistoryService illegalParkingHistoryService;
    private final FireAlertHistoryService fireAlertHistoryService;
    private final CarHistoryService carHistoryService;


    /**
     * 고장 등록
     */
    @PostMapping("/create/fault")
    public ResponseEntity<?> createFaultHistory(@RequestBody @Valid HistoryCreateFaultRequestDTO historyCreateFaultRequestDTO,
                                                BindingResult bindingResult) {
        if(bindingResult.hasErrors()){
            StringBuilder errorMessage = new StringBuilder();
            for (ObjectError error : bindingResult.getAllErrors()) {
                errorMessage.append(error.getDefaultMessage()).append("; ");
            }
            return ResponseEntity.badRequest().body(errorMessage.toString());
        }
        try {
            faultHistoryService.enroll(historyCreateFaultRequestDTO);
            String message = "고장처리 하셨습니다.";
            return ResponseEntity.ok(message);
        }catch (IllegalAccessError error){
            String errorMessage = "등록에 실패하였습니다: " + error.getMessage(); // 오류 메시지 포맷
            //500으로 메시지 고정, 위에서 유효성검사 하기때문에
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }


    /**
     * 고장 업데이트(수정, 고장해결)
     */
    @PutMapping("/update/fault/{chargeId}")
    public ResponseEntity<?> updateFaultHistory(@PathVariable Long chargeId){

        try {
            faultHistoryService.update(chargeId);
            String message = "정상처리 하셨습니다.";
            return ResponseEntity.ok(message);
        }catch (IllegalAccessError error){
            String errorMessage = "수정에 실패하였습니다: " + error.getMessage(); // 오류 메시지 포맷
            //500으로 메시지 고정, 위에서 유효성검사 하기때문에
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }

    }






    /**
     * 고장 이력 조회
     */
    @PostMapping("/history/fault")
    public ResponseEntity<?> selectFaultHistory(@RequestHeader("Authorization") String authorizationHeader,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "recordTime", name = "sortField") String sortField,
                                                @RequestParam(defaultValue = "asc", name = "sortDirection") String sortDirection,
                                                @Valid @RequestBody HistoryOrderRequestDTO historyOrderRequestDTO) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 토큰 형식입니다.");
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        return faultHistoryService.faultSelect(accessToken, historyOrderRequestDTO, page, sortField, sortDirection);
    }


    /**
     * 화재위험 이력 조회
     */
    @PostMapping("/history/fire")
    public ResponseEntity<?> selectFireAlert(@RequestHeader("Authorization") String authorizationHeader,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "recordTime", name = "sortField") String sortField,
                                                @RequestParam(defaultValue = "asc", name = "sortDirection") String sortDirection,
                                                @Valid @RequestBody HistoryOrderRequestDTO historyOrderRequestDTO) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 토큰 형식입니다.");
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        return fireAlertHistoryService.fireSelect(accessToken, historyOrderRequestDTO, page, sortField, sortDirection);
    }


    /**
     * 불법 주정차 이력 조회
     */
    @PostMapping("/history/illegal")
    public ResponseEntity<?> selectIllegalParking(@RequestHeader("Authorization") String authorizationHeader,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "recordTime", name = "sortField") String sortField,
                                                @RequestParam(defaultValue = "asc", name = "sortDirection") String sortDirection,
                                                @Valid @RequestBody HistoryOrderRequestDTO historyOrderRequestDTO) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 토큰 형식입니다.");
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        return illegalParkingHistoryService.illegalParkingSelect(accessToken, historyOrderRequestDTO, page, sortField, sortDirection);
    }


    /**
     * 차량 주차 이력 조회
     */
    @PostMapping("/history/car")
    public ResponseEntity<?> selectCarHistory(@RequestHeader("Authorization") String authorizationHeader,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "inTime", name = "sortField") String sortField,
                                                  @RequestParam(defaultValue = "asc", name = "sortDirection") String sortDirection,
                                                  @Valid @RequestBody HistoryOrderRequestDTO historyOrderRequestDTO) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 토큰 형식입니다.");
        }

        String accessToken = authorizationHeader.substring(7).trim(); // ✅ "Bearer " 제거 후 공백 제거

        return carHistoryService.carSelect(accessToken, historyOrderRequestDTO, page, sortField, sortDirection);

    }


    /**
     * 화재 사후처리 입력
     */
    @PostMapping("/history/fire/reason")
    public ResponseEntity<?> fireReason(@RequestBody HistoryReasonDTO dto) {
        fireAlertHistoryService.fireReasonChk(dto);

        return ResponseEntity.ok(202);
    }





}
