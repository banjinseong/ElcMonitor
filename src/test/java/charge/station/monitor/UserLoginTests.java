package charge.station.monitor;

import charge.station.monitor.config.jwt.JwtUtil;
import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.dto.user.JoinRequestDTO;
import charge.station.monitor.dto.user.LoginRequestDTO;
import charge.station.monitor.dto.user.UserTokenResponseDTO;
import charge.station.monitor.repository.UserRepository;
import charge.station.monitor.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback
public class UserLoginTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @BeforeEach
    public void clearRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushDb(); // 전체 삭제
    }


    private JoinRequestDTO createTestUser() {
        JoinRequestDTO dto = new JoinRequestDTO("testuser", "Password1!", "홍길동", "테스트회사", "test@example.com");
        userService.join(dto);
        return dto;
    }

    @Test
    public void 유저_정상_로그인_테스트() throws Exception{
        //given
        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());

        //when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);



        //then
        Assertions.assertNotNull(userTokenResponseDTO.getAccessToken());
        Assertions.assertNotNull(userTokenResponseDTO.getRefreshToken());

    }


    @Test
    public void 유저_정상_토큰갱신_테스트() throws Exception{
        //given
        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());
        UserTokenResponseDTO loginToken = userService.login(loginDTO);// access + refresh 등록됨

        //when
        String newAccessToken = userService.refreshAccessToken(loginToken.getRefreshToken());

        //then
        Assertions.assertNotNull(newAccessToken); // 새로운 토큰은 널값이면 안됨.
        Assertions.assertNotEquals(loginToken.getAccessToken(), newAccessToken); // 새 토큰은 기존과 달라야 함
    }


    @Test
    public void 유저_오류_아이디없음_테스트() throws Exception{
        //given
        LoginRequestDTO loginDTO = new LoginRequestDTO("no_such_user", "Password1!");

        //when & then
        CustomException ex = Assertions.assertThrows(CustomException.class, () -> userService.login(loginDTO));
        Assertions.assertEquals("아이디가 존재하지 않습니다.", ex.getMessage());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());

    }

    @Test
    public void 유저_오류_비번틀림_테스트() throws Exception{
        //given
        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), "WrongPassword!");

        //when & then
        CustomException ex = Assertions.assertThrows(CustomException.class, () -> userService.login(loginDTO));
        Assertions.assertEquals("비밀번호가 일치하지 않습니다.", ex.getMessage());
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());

    }

    @Test
    public void 유저_정상_로그아웃_테스트() throws Exception{
        //given
        JoinRequestDTO dto = createTestUser();
        LoginRequestDTO loginDTO = new LoginRequestDTO(dto.getLoginId(), dto.getPassword());


        //when
        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginDTO);
        Long userId = jwtUtil.getUserId(userTokenResponseDTO.getAccessToken());
        userService.logout(userTokenResponseDTO.getAccessToken());


        //then
        // 1. 다른 페이지 접근 불가능해져야함
        Object lastActivityTime = redisTemplate.opsForHash().get(userId.toString(), "last_activity_time");
        Assertions.assertNull(lastActivityTime);

        // 2. 로그아웃 이후 Refresh Token으로 재발급 시도 → 실패해야 함
        Assertions.assertThrows(CustomException.class, () -> {
            userService.refreshAccessToken(userTokenResponseDTO.getRefreshToken());
        });

    }

}