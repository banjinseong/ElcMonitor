package charge.station.monitor.dto.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDto {
    private HttpStatus status;
    private int code;
    private String message;
    private LocalDateTime timestamp; // 현재 시간 자동 설정

}