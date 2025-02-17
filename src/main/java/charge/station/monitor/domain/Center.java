package charge.station.monitor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class Center {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "center_id")
    private Long centerId;

    @Column(name = "center_name", nullable = false)
    private String centerName;  // 지역이름

    @Column(name = "center_num", nullable = false)
    private String centerNum;  // 센터 번호 (서울_1, 서울_2 등)
}
