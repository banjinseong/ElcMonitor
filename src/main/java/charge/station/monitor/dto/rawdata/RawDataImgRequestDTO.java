package charge.station.monitor.dto.rawdata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RawDataImgRequestDTO {
    private byte[] img; //이미지 파일
    private Long chargeId; //충전소 id번호
}
