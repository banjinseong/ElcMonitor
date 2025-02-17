package charge.station.monitor.repository;

import charge.station.monitor.domain.User;
import charge.station.monitor.domain.UserRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {
    List<UserRegion> findByUser(User user);
}
