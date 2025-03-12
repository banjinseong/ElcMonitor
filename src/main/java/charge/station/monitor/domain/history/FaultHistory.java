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
public class FaultHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fault_history_id")
    private Long faultHistoryId; // 고장 관리 ID

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime; // 감지 시간

    @Column(name = "release_time")
    private LocalDateTime releaseTime; // 해제시간 (고장 해결 주소)

    @Column(name = "proc_sttus")
    private Boolean procSttus; // 사후 조치 여부 (처리 유무)

    @Column(name = "fault_reason")
    private String faultReason; // 고장사유

    @ManyToOne
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge; // 충전소 ID (외래키)

    @Builder
    public FaultHistory(LocalDateTime recordTime, LocalDateTime releaseTime, Boolean procSttus, Charge charge, String faultReason) {
        this.recordTime = recordTime;
        this.releaseTime = releaseTime;
        this.procSttus = procSttus;
        this.charge = charge;
        this.faultReason = faultReason;
    }
    public void enroll(LocalDateTime recordTime,Charge charge, String faultReason){
        this.recordTime = recordTime;
        this.charge = charge;
        this.faultReason = faultReason;
        this.procSttus = false;
    }

    public void update(LocalDateTime recordTime){
        this.recordTime = recordTime;
        this.procSttus = true;
    }
}
