package charge.station.monitor.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HistoryMainResponseDTO<T> {
    private List<T> items;
    private int total;
    private int currentPage;
    private int totalPages;
}