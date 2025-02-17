package charge.station.monitor.config.jwt;

import charge.station.monitor.config.auth.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 🔹 Refresh Token 요청은 필터 적용 제외
        if (requestURI.equals("/user/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            if (jwtUtil.validateToken(token)) {  // ✅ JWT의 진위성 검증 수행
                Long userId = jwtUtil.getUserId(token);

                String userKey = userId.toString();

                Long lastActivityTime = (Long) redisTemplate.opsForHash().get(userKey, "last_activity_time");

                if (lastActivityTime == null) {
                    request.setAttribute("errorMessage", "유효하지 않은 요청입니다. 다시 로그인 해주세요.");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                long currentTime = System.currentTimeMillis();

                // ✅ 일정시간 이상 미활동 시 자동 로그아웃 처리
                if ((currentTime - lastActivityTime) > 1800000) {
                    redisTemplate.delete(userKey); // 🔹 Redis에서 로그아웃 처리
                    request.setAttribute("errorMessage", "장시간 활동이 없어 자동 로그아웃되었습니다.");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                redisTemplate.opsForHash().put(userKey, "last_activity_time", currentTime);


                String username = jwtUtil.getUsername(token);  // ✅ JWT에서 username 가져오기
                String role = jwtUtil.getRole(token);  // ✅ JWT에서 role 가져오기
                List<String> managedRegions = jwtUtil.getManagedRegions(token);  // ✅ JWT에서 managedRegions 가져오기
                // ✅ DB 조회 없이 JWT에서 가져온 정보로 UserDetails 직접 생성
                CustomUserDetails userDetails = new CustomUserDetails(userId, username, "",
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)), managedRegions);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                //토큰에서 고유id, 이름, 관리레벨, 관리구역 꺼내오기 가능.
            }
        }

        filterChain.doFilter(request, response);
    }
}

