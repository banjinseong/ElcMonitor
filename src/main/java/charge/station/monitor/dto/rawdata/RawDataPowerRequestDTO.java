package charge.station.monitor.dto.rawdata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RawDataPowerRequestDTO {
    private double power; //전력값
    private Long chargeId; //충전소 id번호
    /**
     * 이 외의 데이터들이 추가로 생성될 수 있음.
     */
}
