package charge.station.monitor.repository;

import charge.station.monitor.domain.Center;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CenterRepository extends JpaRepository<Center, Long> {
}
