package charge.station.monitor.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyPageDTO {
    private Long userId;
    private String userName;
    private String company;
    private LocalDate createDate;
    private LocalDate updateDate;
}
