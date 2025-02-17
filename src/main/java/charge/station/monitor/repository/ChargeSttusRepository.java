package charge.station.monitor.repository;

import charge.station.monitor.domain.ChargeSttus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeSttusRepository extends JpaRepository<ChargeSttus, Long> {
}
