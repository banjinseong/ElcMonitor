package charge.station.monitor.repository;

import charge.station.monitor.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    Optional<User> findByUserId(Long userId);


    // ✅ 이메일이 존재하는지 확인 (true / false 반환)
    boolean existsByEmail(String email);

    boolean existsByLoginId(String loginId);

    //loginId를 기준으로 email찾기
    @Query("SELECT u.email FROM User u WHERE u.loginId = :loginId")
    Optional<String> findEmailByLoginId(@Param("loginId") String loginId);

    // ✅ 이메일로 유저 조회 (비밀번호 찾기 등에서 사용 가능)
    Optional<User> findByEmail(String email);
}
