package charge.station.monitor.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurePasswordEncoder implements PasswordEncoder {
    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder(); // 안전한 BCrypt 사용

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword); // ✅ 안전한 해싱 적용
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword); // ✅ 안전한 비교 방식
    }
}