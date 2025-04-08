package charge.station.monitor.repository;

import charge.station.monitor.domain.RawDataImg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawDataImgRepository extends JpaRepository<RawDataImg, Long> {
}
