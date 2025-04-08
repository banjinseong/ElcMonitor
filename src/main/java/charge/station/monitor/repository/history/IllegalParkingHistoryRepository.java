package charge.station.monitor.repository.history;

import charge.station.monitor.domain.history.IllegalParkingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IllegalParkingHistoryRepository extends JpaRepository<IllegalParkingHistory, Long> {
}
