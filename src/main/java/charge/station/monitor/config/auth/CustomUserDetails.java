package charge.station.monitor.config.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails extends org.springframework.security.core.userdetails.User {

    // ✅ userId 가져오기
    private Long userId;  // ✅ userId 추가
    private final List<String> managedRegions;

    public CustomUserDetails(Long userId, String username, String password, Collection<? extends GrantedAuthority> authorities,
                             List<String> managedRegions) {
        super(username, password, authorities);
        this.managedRegions = managedRegions;
    }

}
