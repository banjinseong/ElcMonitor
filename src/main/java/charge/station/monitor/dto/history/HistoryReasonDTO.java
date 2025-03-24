package charge.station.monitor.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HistoryReasonDTO {
    private Long id;
    private String reason;
}
