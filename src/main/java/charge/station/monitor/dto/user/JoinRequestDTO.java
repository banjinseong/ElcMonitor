package charge.station.monitor.dto.user;

import charge.station.monitor.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestDTO {

    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$",
            message = "아이디는 영문자(대소문자 구분 없음)와 숫자로 이루어진 3자 ~ 20자의 값이어야 합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Pattern(regexp="(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}",
            message = "비밀번호는 영문 대,소문자와 숫자, 특수기호가 적어도 1개 이상씩 포함된 8자 ~ 20자의 비밀번호여야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    @Pattern(regexp = "^[가-힣]{1,5}$",
            message = "이름은 한글 1자 ~ 5자여야 하며, 띄어쓰기는 포함할 수 없습니다.")
    private String userName;

    @NotBlank(message = "회사명은 필수 입력 값입니다.")
    @Pattern(regexp = "^[가-힣()]{2,15}$",
            message = "회사명은 한글 2자 ~ 15자이며, 괄호만 허용됩니다.")
    private String company;


    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
            message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    public User toEntity() {
        return User.builder()
                .loginId(loginId)
                .password(password)
                .userName(userName)
                .company(company)
                .email(email)
                .build();
    }
}
