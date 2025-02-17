package charge.station.monitor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charge_id")
    private Long chargeId;  // 충전소 ID

    @Column(name = "charge_num", nullable = false, unique = true)
    private String chargeNum;  // 충전소 이름(번호)

    @Column(name = "charge_addr")
    private String chargeAddr;  // 위치 정보 (주소)

    @ManyToOne
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;  // 센터 ID (외래키)

    @OneToOne(mappedBy = "charge", cascade = CascadeType.ALL)
    private ChargeSttus chargeSttus;  // 충전소 현황
}

