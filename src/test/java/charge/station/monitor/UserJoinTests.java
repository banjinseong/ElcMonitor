package charge.station.monitor;


import charge.station.monitor.config.SecurePasswordEncoder;
import charge.station.monitor.domain.User;
import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.dto.user.JoinRequestDTO;
import charge.station.monitor.dto.user.LoginRequestDTO;
import charge.station.monitor.dto.user.UserTokenResponseDTO;
import charge.station.monitor.repository.UserRepository;
import charge.station.monitor.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;



@SpringBootTest
@Transactional
@Rollback
public class UserJoinTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurePasswordEncoder passwordEncoder;

    @Test
    public void 유저_정상_회원가입_테스트() throws Exception{
        //given
        JoinRequestDTO dto = JoinRequestDTO.builder()
                .loginId("testuser1")
                .password("password123!")
                .userName("테스트유저")
                .company("테스트회사")
                .email("testuser@example.com")
                .build();

        //when
        Long savedId = userService.join(dto);

        //then
        User savedUser = userRepository.findById(savedId).orElseThrow();
        Assertions.assertEquals("testuser1", savedUser.getLoginId());
        Assertions.assertEquals("테스트유저", savedUser.getUserName());
        Assertions.assertEquals("테스트회사", savedUser.getCompany());
        Assertions.assertEquals("testuser@example.com", savedUser.getEmail());
        Assertions.assertNotEquals("password123!", savedUser.getPassword()); // 비밀번호는 인코딩됨
    }

    @Test
    public void 유저_오류_회원가입_아이디중복_테스트() throws Exception{
        //given
        JoinRequestDTO dto1 = new JoinRequestDTO
                ("dupUser", "Password1!", "홍길동", "테스트회사", "test1@example.com");
        JoinRequestDTO dto2 = new JoinRequestDTO
                ("dupUser", "Password2!", "김철수", "다른회사", "test2@example.com");

        //when
        userService.join(dto1);

        //then
        Assertions.assertThrows(CustomException.class, () -> userService.join(dto2));
    }

    @Test
    public void 유저_정상_비밀번호변경_테스트() throws Exception{
        //given
        JoinRequestDTO dto1 = new JoinRequestDTO
                ("dupUser1", "Password1!", "홍길동", "테스트회사", "test1@example.com");
        JoinRequestDTO dto2 = new JoinRequestDTO
                ("dupUser2", "Password2!", "김철수", "다른회사", "test2@example.com");

        LoginRequestDTO loginRequestDTO = new LoginRequestDTO(dto2.getLoginId(), dto2.getPassword());
        String newPassword = "NewPassword1!";

        //when
        userService.join(dto1);
        userService.join(dto2);

        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginRequestDTO);

        userService.updatePassword(dto1.getLoginId(), newPassword);
        userService.updatePassword(userTokenResponseDTO.getAccessToken(), dto2.getPassword(), newPassword);
        //then
        User user1 = userRepository.findByLoginId(dto1.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        Assertions.assertTrue(passwordEncoder.matches(newPassword, user1.getPassword()));


        User user2 = userRepository.findByLoginId(dto2.getLoginId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        Assertions.assertTrue(passwordEncoder.matches(newPassword, user2.getPassword()));
    }

    /**
     *  존재하지 않는 아이디, 현재비밀번호오류
     */
    @Test
    public void 유저_오류_비밀번호변경_테스트() throws Exception{
        //given
        JoinRequestDTO dto1 = new JoinRequestDTO
                ("dupUser1", "Password1!", "홍길동", "테스트회사", "test1@example.com");
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO(dto1.getLoginId(), dto1.getPassword());

        //when
        userService.join(dto1);

        UserTokenResponseDTO userTokenResponseDTO = userService.login(loginRequestDTO);

        //then
        //아이디 오류
        Assertions.assertThrows(CustomException.class, () -> userService.updatePassword("123", dto1.getPassword()));

        //현재 비번 오류
        Assertions.assertThrows(CustomException.class, () -> userService.updatePassword(userTokenResponseDTO.getAccessToken(), "123", dto1.getPassword()));

    }
}
