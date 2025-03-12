package charge.station.monitor.repository.history;

import charge.station.monitor.domain.history.FaultHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaultHistoryRepository extends JpaRepository<FaultHistory, Long> {
}
