package charge.station.monitor.service;


import charge.station.monitor.config.SecurePasswordEncoder;
import charge.station.monitor.config.jwt.JwtUtil;
import charge.station.monitor.domain.User;
import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.dto.user.*;
import charge.station.monitor.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SecurePasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EMailService EMailService;



    // ✅ 이메일 인증번호 요청 (signup / find 구분)
    public void requestAuthCode(EMailRequestDTO dto) {
        if (dto.getType().equals("signup") && userRepository.existsByEmail(dto.getEmail())) {
            throw new CustomException("이미 가입된 이메일입니다.", HttpStatus.BAD_REQUEST, 400);
        }else{
            if (!userRepository.existsByLoginId(dto.getLoginId())) {
                throw new CustomException("존재하지 않는 ID입니다.", HttpStatus.BAD_REQUEST, 400);
            }
            Optional<String> userEmail = userRepository.findEmailByLoginId(dto.getLoginId());
            if (userEmail.isEmpty() || !userEmail.get().equals(dto.getEmail())) {
                throw new CustomException("ID에 해당하는 이메일과 일치하지 않습니다.", HttpStatus.BAD_REQUEST, 400);
            }
        }
        EMailService.sendAuthCode(dto.getEmail(), dto.getType()); // 인증번호 전송
    }


    // ✅ 인증번호 검증 (signup / find 구분)
    public void verifyAuthCode(String email, String type, String inputCode) {
        String redisKey = "auth_code:" + type + ":" + email; // "auth_code:signup:email" or "auth_code:reset:email"
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey); // Redis에서 값 가져오기

        if (storedCode == null) {
            throw new CustomException("인증번호가 만료되었거나 요청되지 않았습니다.", HttpStatus.BAD_REQUEST, 400);
        }
        if (!storedCode.equals(inputCode)) {
            throw new CustomException("인증번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED, 401);
        }

        redisTemplate.delete(redisKey); // 인증 성공 후 Redis에서 삭제
    }


    /**
     * 비밀번호 {찾기} 경로를 통한 비밀번호 변경
     */
    @Transactional
    public void updatePassword(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException("유효하지 않은 사용자 정보입니다: " + loginId, HttpStatus.NOT_FOUND, 404));

        user.encodePassword(passwordEncoder.encode(password));
    }

    /**
     * 비밀번호 {변경} 경로를 통한 비밀번호 변경.
     */
    @Transactional
    public void updatePassword(String accessToken, String password, String newPassword){
        if (jwtUtil.validateToken(accessToken)) {
            Long userId = jwtUtil.getUserId(accessToken);
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new CustomException("유효하지 않은 사용자 정보입니다: " + userId, HttpStatus.NOT_FOUND, 404));
            if(!passwordEncoder.matches(password,user.getPassword())){
                throw new CustomException("현재 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED, 404);
            }
            user.encodePassword(passwordEncoder.encode(newPassword));
        } else {
            throw new CustomException("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED, 401);
        }
    }



    /**
     * 보강 수정 해야됨.
     */
    @Transactional
    public Long join(JoinRequestDTO joinRequestDTO){
        User user = joinRequestDTO.toEntity();
        validateDuplicateUser(user);
        user.encodePassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return user.getUserId();
    }




    private void validateDuplicateUser(User user){
        userRepository.findByLoginId(user.getLoginId()).ifPresent(user1 -> {
            throw new CustomException("이미 존재하는 아이디입니다.", HttpStatus.CONFLICT, 40901);
        });
    }


    /**
     * 로그인 서비스 동작
     */
    public UserTokenResponseDTO login(LoginRequestDTO loginRequestDTO){
        String loginId = loginRequestDTO.getLoginId();
        String password = loginRequestDTO.getPassword();

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException("아이디가 존재하지 않습니다.", HttpStatus.NOT_FOUND, 404));

        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new CustomException("비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED, 401);
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


        return new UserTokenResponseDTO(accessToken, refreshToken);
    }


    /**
     * Access Token이 만료되었을 때 Refresh Token을 검증하여 새로운 Access Token 발급
     */
    public String refreshAccessToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException("유효하지 않은 Refresh Token입니다.", HttpStatus.UNAUTHORIZED, 401);
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        if (userId == null) {
            throw new CustomException("토큰 사용자 정보를 확인할 수 없습니다.", HttpStatus.UNAUTHORIZED, 401);
        }

        String storedToken = (String) redisTemplate.opsForHash().get(userId.toString(), "refresh_token");
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new CustomException("Refresh Token이 일치하지 않거나 세션이 만료되었습니다.", HttpStatus.UNAUTHORIZED, 402);
        }

        JwtUserInfo info = new JwtUserInfo(userId,
                jwtUtil.getUsername(refreshToken),
                jwtUtil.getRole(refreshToken),
                jwtUtil.getManagedRegions(refreshToken));

        return jwtUtil.createAccessToken(info);
    }


    /**
     * 로그아웃: Redis에서 Refresh Token 삭제
     */
    public void logout(String accessToken) {
        if (!jwtUtil.validateToken(accessToken)) {
            throw new CustomException("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED, 401);
        }

        Long userId = jwtUtil.getUserId(accessToken);
        redisTemplate.delete(userId.toString()); // ✅ refresh token + last_activity_time 전부 삭제
    }


    /**
     * 마이페이지
     */
    public MyPageDTO myPage(String accessToken){

        Long userId;

        if (jwtUtil.validateToken(accessToken)) {
            userId = jwtUtil.getUserId(accessToken);
        } else {
            throw new CustomException("토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED, 401);
        }

        User user = userRepository.findByUserId(userId).orElseThrow(
                () -> new CustomException("아이디가 존재하지 않습니다.", HttpStatus.NOT_FOUND, 404));

        MyPageDTO myPageDTO = new MyPageDTO();
        myPageDTO.setUserId(user.getUserId());
        myPageDTO.setUserName(user.getUserName());
        myPageDTO.setCompany(user.getCompany());
        myPageDTO.setCreateDate(user.getCreateDate());
        myPageDTO.setUpdateDate(user.getUpdateDate());

        return myPageDTO;

    }


    /**
     * 마이페이지 수정
     * 단순 정보 전체 수정.
     * (이름, 회사, 수정일)받아오는 정보는 이름, 회사 뿐.
     */
    @Transactional
    public void myPageUpdate(String accessToken, MyPageDTO myPageDTO){

        User user = userRepository.findByUserId(myPageDTO.getUserId()).orElseThrow(
                () -> new CustomException("아이디가 존재하지 않습니다.", HttpStatus.NOT_FOUND, 404));

        user.pageUpdate(myPageDTO.getUserName(), myPageDTO.getCompany());

    }


}
