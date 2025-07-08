package charge.station.monitor.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTokenResponseDTO {
    private String accessToken;
    private String refreshToken;
}
