package charge.station.monitor.controller;

import charge.station.monitor.domain.Center;
import charge.station.monitor.domain.Charge;
import charge.station.monitor.repository.CenterRepository;
import charge.station.monitor.repository.ChargeRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("charge/*")
public class ChargeController {

    private final ChargeRepository chargeRepository;
    private final CenterRepository centerRepository;

    @PostMapping("create")
    public ResponseEntity<?> createCharge(HttpServletRequest request) {

        String chargeId = request.getHeader("Charge-ID");


        Center center = new Center(Long.parseLong(chargeId), "의왕", "의왕_1");

        centerRepository.save(center);


        Charge charge = Charge.builder()
                .chargeId(Long.parseLong(chargeId))
                .chargeNum("t-1")
                .instlLc("삼동")
                .center(center)
                .build();

        chargeRepository.save(charge);

        return ResponseEntity.ok().build();
    }
}
