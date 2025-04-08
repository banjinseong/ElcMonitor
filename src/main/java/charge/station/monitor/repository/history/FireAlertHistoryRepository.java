package charge.station.monitor.repository.history;

import charge.station.monitor.domain.history.FireAlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FireAlertHistoryRepository extends JpaRepository<FireAlertHistory, Long> {
}
