package charge.station.monitor.service;


import charge.station.monitor.config.SimplePasswordEncoder;
import charge.station.monitor.config.jwt.JwtUtil;
import charge.station.monitor.domain.User;
import charge.station.monitor.dto.user.JwtUserInfo;
import charge.station.monitor.dto.user.UserJoinRequestDTO;
import charge.station.monitor.dto.user.UserLoginReqeustDTO;
import charge.station.monitor.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SimplePasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 보강 수정 해야됨.
     */
    @Transactional
    public Long join(UserJoinRequestDTO userJoinRequestDTO){
        User user = userJoinRequestDTO.toEntity();
        validateDuplicateUser(user);
        user.encodePassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return user.getUserId();
    }




    private void validateDuplicateUser(User user){
        userRepository.findByLoginId(user.getLoginId()).ifPresent(user1 -> {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        });
    }




    public String login(UserLoginReqeustDTO userLoginReqeustDTO){
        String loginId = userLoginReqeustDTO.getLoginId();
        String password = userLoginReqeustDTO.getPassword();
        User user = userRepository.findByLoginId(loginId).orElse(null);
        if(user==null){
            throw new UsernameNotFoundException("아이디가 존재하지 않습니다.");
        }
        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        JwtUserInfo info = new JwtUserInfo(user.getUserId()
                ,user.getUserName(), user.getUserRole().toString(),
                user.getUserRegions().stream()
                        .map(userRegion -> userRegion.getRegion().getRegionName())
                        .collect(Collectors.toList()));

        String accessToken = jwtUtil.createAccessToken(info);
        String refreshToken = jwtUtil.createRefreshToken(info);

        // ✅ Redis에 Refresh Token 및 마지막 활동 시간 저장
        redisTemplate.opsForHash().put(user.getUserId().toString(), "refresh_token", refreshToken);
        redisTemplate.opsForHash().put(user.getUserId().toString(), "last_activity_time", System.currentTimeMillis());


        return accessToken;
    }


    /**
     * Access Token이 만료되었을 때 Refresh Token을 검증하여 새로운 Access Token 발급
     */
    public String refreshAccessToken(String accessToken, HttpServletRequest request, HttpServletResponse response) throws IOException{
        Long userId;

        if (jwtUtil.validateToken(accessToken)) {
            userId = jwtUtil.getUserId(accessToken);
        } else {
            userId = jwtUtil.getUserIdIfSignatureValid(accessToken);
        }

        // ✅ 변조된 토큰이면 차단 (에러 메시지를 request에 저장)
        if (userId == null) {
            request.setAttribute("errorMessage", "유효하지 않은 토큰입니다.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        // ✅ Redis에서 Refresh Token 존재 여부 확인
        String storedRefreshToken = (String) redisTemplate.opsForHash().get(userId.toString(), "refresh_token");
        System.out.println(storedRefreshToken);

        if (storedRefreshToken == null) {
            request.setAttribute("errorMessage", "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        // ✅ 새로운 Access Token 발급
        JwtUserInfo info = new JwtUserInfo(userId, jwtUtil.getUsername(accessToken),
                jwtUtil.getRole(accessToken), jwtUtil.getManagedRegions(accessToken));

        return jwtUtil.createAccessToken(info);
    }


    /**
     * 로그아웃: Redis에서 Refresh Token 삭제
     */
    public void logout(String accessToken, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (jwtUtil.validateToken(accessToken)) {
            Long userId = jwtUtil.getUserId(accessToken);
            redisTemplate.delete(userId.toString());  // ✅ Redis에서 Refresh Token 삭제
        } else {
            request.setAttribute("errorMessage", "유효하지 않은 토큰입니다.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }


}
