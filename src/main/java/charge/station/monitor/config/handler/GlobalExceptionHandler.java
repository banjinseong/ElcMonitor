package charge.station.monitor.config.handler;

import charge.station.monitor.dto.error.CustomException;
import charge.station.monitor.dto.error.ErrorResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponseDto> handleCustomException(CustomException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                ex.getStatus().value(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }
}
