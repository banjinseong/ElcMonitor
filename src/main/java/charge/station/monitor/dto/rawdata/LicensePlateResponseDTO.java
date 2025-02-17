package charge.station.monitor.dto.rawdata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LicensePlateResponseDTO {
    private boolean carExists;
    private String carNum;
}
