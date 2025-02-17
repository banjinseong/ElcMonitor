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
public class CarHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_history_id", nullable = false)
    private Long carHistoryId;

    @Column(name = "car_num", nullable = true, length = 255)
    private String carNum;

    @Column(name = "in_time", nullable = false)
    private LocalDateTime inTime;

    @Column(name = "out_time", nullable = true)
    private LocalDateTime outTime;

    @ManyToOne
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge;  // 충전소 ID (외래키)

    @Column(name = "charge_start_time", nullable = true)
    private LocalDateTime chargeStartTime;

    @Column(name = "charge_end_time", nullable = true)
    private LocalDateTime chargeEndTime;


    @Builder
    private CarHistory(String carNum, LocalDateTime inTime, LocalDateTime outTime,
                       Charge charge, LocalDateTime chargeStartTime, LocalDateTime chargeEndTime) {
        this.carNum = carNum;
        this.inTime = inTime;
        this.outTime = outTime;
        this.charge = charge;
        this.chargeStartTime = chargeStartTime;
        this.chargeEndTime = chargeEndTime;
    }

    // 정적 팩토리 메서드 (입차)
    public static CarHistory createEntry(String carNum, Charge charge) {
        return CarHistory.builder()
                .carNum(carNum)
                .inTime(LocalDateTime.now())
                .charge(charge)
                .build();
    }

    // 출차 (out_time 추가)
    public void exit(LocalDateTime outTime) {
        this.outTime = outTime;
    }

    // 충전 시작
    public void startCharging(LocalDateTime chargeStartTime) {
        this.chargeStartTime = chargeStartTime;
    }

    // 충전 완료
    public void completeCharging(LocalDateTime chargeEndTime) {
        this.chargeEndTime = chargeEndTime;
    }
}