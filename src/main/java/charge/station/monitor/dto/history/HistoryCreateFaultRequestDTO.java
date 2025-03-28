package charge.station.monitor.dto.history;

import charge.station.monitor.domain.Charge;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HistoryCreateFaultRequestDTO {
    //Charge->chargeId로 변경
    private Long chargeId;
    private String faultReason;
}
