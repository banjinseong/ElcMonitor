package charge.station.monitor.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HistoryReadFaultResponseDTO {
    private Long faultHistoryId;
    private LocalDateTime recordTime;
    private LocalDateTime releaseTime;
    private Boolean procSttus;
    private String faultReason;
    private String chargeNm;  // 충전소 이름만 추출
}
