package charge.station.monitor;


import charge.station.monitor.service.RawDataService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback
public class RawDataTests {

    @Autowired
    private RawDataService rawDataService;


    @Test
    public void 방화문_조회_테스트() throws Exception{
        //given

        //when
        //then

    }


}
