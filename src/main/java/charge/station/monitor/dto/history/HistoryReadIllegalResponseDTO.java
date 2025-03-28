package charge.station.monitor.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HistoryReadIllegalResponseDTO {
    private Long illegalParkingHistoryId;
    private String carNum;
    private LocalDateTime recordTime;
    private Boolean procSttus;
    private String type; //화재감지 판단 기준
    private String chargeNm;  // 충전소 이름만 추출
}
