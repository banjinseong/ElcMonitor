package charge.station.monitor.repository.history;

import charge.station.monitor.domain.Charge;
import charge.station.monitor.domain.history.CarHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CarHistoryRepository extends JpaRepository<CarHistory, Long> {
    // 가장 최근에 입차한 기록 중 출차하지 않은 데이터 조회
    @Query("SELECT ch FROM CarHistory ch " +
            "WHERE ch.charge = :charge " +
            "AND ch.outTime IS NULL " +
            "ORDER BY ch.inTime DESC LIMIT 1")
    Optional<CarHistory> findLatestEntryByCharge(@Param("charge") Charge charge);
}
