package charge.station.monitor.domain.history;

import charge.station.monitor.domain.Charge;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
public class FireAlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fire_alert_history_id")
    private Long fireAlertHistoryId; // 화재 위험 감지 ID

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime; // 감지 시간

    @Column(name = "proc_sttus")
    private Boolean procSttus; // 사후 조치 여부 (처리 유무)

    @ManyToOne
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge; // 충전소 ID (외래키)

    @Builder
    public FireAlertHistory(LocalDateTime recordTime, Boolean procSttus, Charge charge) {
        this.recordTime = recordTime;
        this.procSttus = procSttus;
        this.charge = charge;
    }
}
