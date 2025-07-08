package charge.station.monitor;


import charge.station.monitor.domain.User;
import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.dto.user.JoinRequestDTO;
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



}
