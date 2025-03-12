package charge.station.monitor.repository;

import charge.station.monitor.domain.ChargeStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeStatRepository extends JpaRepository<ChargeStat, Long> {
}
