package charge.station.monitor.controller;

import charge.station.monitor.service.RawDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("rawData/*")
public class RawDataController {

    private final RawDataService rawDataService;
    
}
