package charge.station.monitor.repository.history;

import charge.station.monitor.domain.history.IllegalParkingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IllegalParkingHistoryRepository extends JpaRepository<IllegalParkingHistory, Long> {
}
