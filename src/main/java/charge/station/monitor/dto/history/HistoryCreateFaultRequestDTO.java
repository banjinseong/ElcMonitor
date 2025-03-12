package charge.station.monitor.dto.history;

import charge.station.monitor.domain.Charge;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HistoryCreateFaultRequestDTO {
    private Charge charge;
    private String faultReason;
}
