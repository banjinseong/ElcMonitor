package charge.station.monitor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class Controller {

    @GetMapping("main")
    public String mai22n() {

        return "This is main page";  // ✅ JSON 응답
    }
}
