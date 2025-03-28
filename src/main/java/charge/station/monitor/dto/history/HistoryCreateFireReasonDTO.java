package charge.station.monitor.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HistoryCreateFireReasonDTO {
    private List<Long> ids;


}
