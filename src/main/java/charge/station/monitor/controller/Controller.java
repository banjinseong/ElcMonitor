package charge.station.monitor.controller;

import charge.station.monitor.service.EMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class Controller {

    private final EMailService EMailService;

    @GetMapping("main")
    public String mai22n() {

        return "This is main page";  // ✅ JSON 응답
    }



}
