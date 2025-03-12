package charge.station.monitor.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
public class ChargeStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charge_stat_id")
    private Long chargeStatId; // 통계 ID

    @Column(name = "date", nullable = false)
    private LocalDateTime date; // 날짜(하루 단위)

    @Column(name = "avg_use_time")
    private Float avgUseTime; // 평균 이용 시간

    @Column(name = "use_time_json")
    private String useTimeJson; // 이용시간 분포 (JSON)

    @ManyToOne
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge; // 충전소 ID (외래키)

    @Builder
    public ChargeStat(LocalDateTime date, Float avgUseTime, String useTimeJson, Charge charge) {
        this.date = date;
        this.avgUseTime = avgUseTime;
        this.useTimeJson = useTimeJson;
        this.charge = charge;
    }
}
