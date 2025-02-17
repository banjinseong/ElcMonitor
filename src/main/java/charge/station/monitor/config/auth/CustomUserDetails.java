package charge.station.monitor.config.auth;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails extends org.springframework.security.core.userdetails.User {

    private Long userId;  // ✅ userId 추가
    private List<String> managedRegions;

    public CustomUserDetails(Long userId, String username, String password, Collection<? extends GrantedAuthority> authorities,
                             List<String> managedRegions) {
        super(username, password, authorities);
        this.managedRegions = managedRegions;
    }

    public Long getUserId() {  // ✅ userId 가져오기
        return userId;
    }

    public List<String> getManagedRegions() {
        return managedRegions;
    }
}
