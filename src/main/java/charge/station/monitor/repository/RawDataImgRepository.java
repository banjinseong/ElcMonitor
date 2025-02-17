package charge.station.monitor.repository;

import charge.station.monitor.domain.RawDataImg;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawDataImgRepository extends JpaRepository<RawDataImg, Long> {
}
