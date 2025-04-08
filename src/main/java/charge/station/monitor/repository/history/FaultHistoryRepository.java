package charge.station.monitor.repository.history;

import charge.station.monitor.domain.history.FaultHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaultHistoryRepository extends JpaRepository<FaultHistory, Long> {
}
