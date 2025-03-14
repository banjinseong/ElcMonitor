package charge.station.monitor.dto.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDto {
    private int status;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now(); // 현재 시간 자동 설정

}