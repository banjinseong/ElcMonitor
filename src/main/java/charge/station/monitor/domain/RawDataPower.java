package charge.station.monitor.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
public class RawDataPower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_data_id")
    private Long rawDataId;  // 현장 데이터 ID

    @ManyToOne
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge;  // 충전소 ID (외래키)

    @Column(name = "power", nullable = false)
    private double power;  // 전력 값

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;  // 현장 시간


    @Builder
    public RawDataPower(Charge charge, double power) {
        this.charge = charge;
        this.power = power;
        this.recordTime = LocalDateTime.now();

    }
}
