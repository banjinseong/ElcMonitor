package charge.station.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChargeRuntimeDetailDTO {

    private LocalDateTime chargeStartTime;  // 충전 시작 시간
    private Boolean powerSttus;             // 충전 유무
    private Boolean faultSttus;             // 고장 유무
    private LocalDateTime inTime;           // 입차 시간
    private String imgPath;                 // 이미지 경로
    private String carNum;                  // 차량 번호
}
