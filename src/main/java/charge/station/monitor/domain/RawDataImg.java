package charge.station.monitor.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
public class RawDataImg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_data_id")
    private Long rawDataId;  // 현장 데이터 ID

    @ManyToOne
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge;  // 충전소 ID (외래키)

    @Column(name = "img_path")
    private String imgPath;  // 이미지 저장 위치

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;  // 현장 시간


    @Builder
    public RawDataImg(Charge charge, String imgPath) {
        this.charge = charge;
        this.imgPath = imgPath;
        this.recordTime = LocalDateTime.now();
    }

}
