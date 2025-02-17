package charge.station.monitor.dto.user;

import charge.station.monitor.domain.UserRole;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserInfo {
    private Long userId;                // 사용자 ID
    private String username;            // 사용자 이름
    private String role;                // 권한 등급
    private List<String> managedRegions; // 관리 지역 리스트

}
