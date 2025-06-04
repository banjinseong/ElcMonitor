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
public class IllegalParkingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "illegal_parking_history_id")
    private Long illegalParkingHistoryId; // 불법 주정차 ID

    @Column(name = "car_num")
    private String carNum; // 차량 번호

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime; // 적발 시간

    @Column(name = "type", nullable = false)
    private String type; // 적발 사유 (일반차량 진입, 점유시간 초과 등)

    @Column(name = "proc_sttus")
    private Boolean procSttus; // 사후 조치 여부 (처리 유무)

    @ManyToOne
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge; // 충전소 ID (외래키)

    @Builder
    public IllegalParkingHistory(String carNum, LocalDateTime recordTime, String type, Boolean procSttus, Charge charge) {
        this.carNum = carNum;
        this.recordTime = recordTime;
        this.type = type;
        this.procSttus = procSttus;
        this.charge = charge;
    }

    public void writeProcSttus() {
        this.procSttus = true;
    }
}
