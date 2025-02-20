package charge.station.monitor.controller;

import charge.station.monitor.dto.cache.ChargeCacheDTO;
import charge.station.monitor.dto.rawdata.RawDataImgRequestDTO;
import charge.station.monitor.dto.rawdata.RawDataPowerRequestDTO;
import charge.station.monitor.service.RawDataService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("rawData/*")
public class RawDataController {

    private final RawDataService rawDataService;



    /**
     * 이미지 컨트롤러
     */
    @PostMapping("imgConnection")
    public ResponseEntity<?> rawDataSaveImg(@RequestBody byte[] image, HttpServletRequest request){
        // 요청 헤더에서 "Camera-ID" 가져오기
        String chargeId = request.getHeader("Charge-ID");

        //이미지와 충전소 id dto세팅.
        RawDataImgRequestDTO rawDataImgRequestDTO = new RawDataImgRequestDTO();
        rawDataImgRequestDTO.setImg(image);
        rawDataImgRequestDTO.setChargeId(Long.parseLong(chargeId));

        //이미지 저장 후 현재 이미지의 정보 반환.
        //현재 이미지의 정보를 가지고 이전 캐시정보와 비교.(비교 후 현황이든 이력 db수정)
        rawDataService.plateForImg(rawDataImgRequestDTO);

        return ResponseEntity.ok().build();

    }



    /**
     * 전력량 컨트롤러
     */
    @PostMapping("powerConnection")
    public void rawDataSavePower(@RequestBody RawDataPowerRequestDTO rawDataPowerRequestDTO){
        rawDataService.savePower(rawDataPowerRequestDTO);
    }

}
