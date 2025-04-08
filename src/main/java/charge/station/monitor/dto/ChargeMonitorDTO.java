package charge.station.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChargeMonitorDTO {
    private Long chargeId;
    private String chargeNum;
    private String instlLc;
    private String companyNm;
    private String modelNm;
    private String centerName;
    private Boolean faultSttus;
    private Boolean seatSttus;
}
