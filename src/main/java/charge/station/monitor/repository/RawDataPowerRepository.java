package charge.station.monitor.repository;

import charge.station.monitor.domain.RawDataPower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawDataPowerRepository extends JpaRepository<RawDataPower, Long> {
}
