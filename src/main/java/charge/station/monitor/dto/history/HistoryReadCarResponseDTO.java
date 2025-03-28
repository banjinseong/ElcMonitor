package charge.station.monitor.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HistoryReadCarResponseDTO {
    private Long carHistoryId;
    private String carNum;
    private LocalDateTime recordTime;
    private LocalDateTime releaseTime;
    private String chargeNm;  // 충전소 이름만 추출
    private LocalDateTime chargeStartTime;
    private LocalDateTime chargeEndTime;
}
