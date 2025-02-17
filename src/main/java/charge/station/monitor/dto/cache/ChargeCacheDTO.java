package charge.station.monitor.dto.cache;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChargeCacheDTO {
    private Long chargeId;
    private boolean carExists;
    private double power;
    private String carNum;
}
