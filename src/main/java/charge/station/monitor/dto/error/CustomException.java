package charge.station.monitor.dto.error;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@Getter
public class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final int code;

    public CustomException(String message, HttpStatus status, int code) {
        super(message);
        this.status = status;
        this.code = code;
        log.error("예외 발생: {} (HTTP 상태: {})", message, status);
    }
}