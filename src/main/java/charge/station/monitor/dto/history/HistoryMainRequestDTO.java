package charge.station.monitor.dto.history;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HistoryMainRequestDTO {

    private Long centerId; //센터번호
    private Long chargeId; //충전소 번호

    private LocalDateTime startTime; //시작기간
    private LocalDateTime endTime; //종료 기간

    private String carNum; //차량 번호
}
