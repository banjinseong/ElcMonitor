package charge.station.monitor.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginReqeustDTO {

    @NotNull(message = "아이디 입력은 필수입니다.")
    private String loginId;


    @NotNull(message = "패스워드 입력은 필수입니다.")
    private String password;
}
