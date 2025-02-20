package charge.station.monitor.domain;

import jakarta.persistence.*;
import lombok.Builder;
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

    @Column(name = "instl_lc")
    private String instlLc;  // 위치 정보 (주소)

    @Column(name = "instl_de")
    private String instlDe;  // 설치날짜

    @Column(name = "company_nm")
    private String companyNm;  // 충전기 회사

    @Column(name = "model_nm")
    private String modelNm;  // 충전기 모델

    @ManyToOne
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;  // 센터 ID (외래키)

    @OneToOne(mappedBy = "charge", cascade = CascadeType.ALL)
    private ChargeSttus chargeSttus;  // 충전소 현황

    @Builder
    public Charge(Long chargeId, String chargeNum, String instlLc, Center center){
        this.chargeId = chargeId;
        this.chargeNum = chargeNum;
        this.instlLc = instlLc;
        this.center = center;
    }
}

