package charge.station.monitor.config;


import charge.station.monitor.config.filter.ApiKeyFilter;
import charge.station.monitor.config.handler.CustomAccessDeniedHandler;
import charge.station.monitor.config.handler.CustomAuthenticationEntryPoint;
import charge.station.monitor.config.jwt.JwtAuthFilter;
import charge.station.monitor.config.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final RedisTemplate<String, Object> redisTemplate;


    /**
     * RawData 전용 엔드포인트
     */
    private static final String[] RAWDATA_WHITELIST = {
            "/rawData/**" // 현장 장비에서 요청하는 엔드포인트
    };


    /**
     * 유저 전용 접근 권한
     */
    private static final String[] USER_WHITELIST = {
            "main/**"
    };

    /**
     * 관리자 전용 접근 권하 ㄴ만들기
     */
    private static final String[] ADMIN_WHITELIST = {
    };


    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 권한(hasRole)을 사용할때는 자동으로 앞에 ROLE_를 붙여서 검사한다..
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        //CSRF, CORS
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(Customizer.withDefaults());

        //세션 관리 상태 없음으로 구성, Spring Security가 세션 생성 or 사용 X
        http.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS));

        //FormLogin, BasicHttp 비활성화
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);


        //JwtAuthFilter를 UsernamePasswordAuthenticationFilter 앞에 추가
        http.addFilterBefore(new JwtAuthFilter(jwtUtil,redisTemplate), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new ApiKeyFilter(), UsernamePasswordAuthenticationFilter.class); // 🔹 API Key 필터 추가

        http.exceptionHandling((exceptionHandling) -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
        );

        // 권한 규칙 작성
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/user/login", "/user/join", "/user/refresh").permitAll() // 🔹 로그인, 회원가입, 리프레시 토큰은 누구나 접근 가능
                .requestMatchers("/user/logout").authenticated() // 🔹 로그아웃은 인증된 사용자만 가능
                .requestMatchers(ADMIN_WHITELIST).hasRole("ADMIN")
                .requestMatchers(USER_WHITELIST).hasRole("USER")
                .requestMatchers(RAWDATA_WHITELIST).permitAll() // 위에 api키에서 인증 완료되어야 접근 가능하기때문,.
                .anyRequest().denyAll() // 그 외 모든 요청은 접근 불가.

        );

        return http.build();
    }

}
