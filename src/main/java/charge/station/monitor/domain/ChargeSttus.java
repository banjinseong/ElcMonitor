package charge.station.monitor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class ChargeSttus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charge_id")
    private Long chargeId;  // 충전소 ID (외래키)

    @OneToOne
    @MapsId
    @JoinColumn(name = "charge_id")
    private Charge charge;  // 충전소 엔티티 참조

    @Column(name = "seat_sttus")
    private Boolean seatSttus = false;  // 자리 유무 (기본값: false)

    @Column(name = "power_sttus")
    private Boolean powerSttus = false;  // 충전 유무 (기본값: false)

    @Column(name = "fault_sttus")
    private Boolean faultSttus = false;  // 고장 유무 (기본값: false)

    // 자리 유무 변경 (입차)
    public void enter() {
        this.seatSttus = true;
    }

    // 자리 유무 변경 (출차)
    public void exit() {
        this.seatSttus = false;
    }

    // 충전 시작
    public void startCharging() {
        this.powerSttus = true;
    }

    // 충전 중지
    public void stopCharging() {
        this.powerSttus = false;
    }

    // 고장 발생
    public void reportFault() {
        this.faultSttus = true;
    }

    // 고장 복구
    public void recoverFault() {
        this.faultSttus = false;
    }
}
