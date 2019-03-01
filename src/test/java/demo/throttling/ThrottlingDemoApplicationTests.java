package demo.throttling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ThrottlingDemoApplicationTests {

    private static String user1Token = "johnToken1";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void greetingShouldReturnDefaultMessage() {
        callEndpoint(user1Token, true); // first call takes longer

        // to show that difference in time is insignificant
        System.out.println("Statistics with token check: " + getStatisticsForCalls(false));
        System.out.println("Statistics without token check: " + getStatisticsForCalls(true));
    }

    private LongSummaryStatistics getStatisticsForCalls(boolean skipTokenCheck) {
        List<Long> timeList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            long time = System.currentTimeMillis();
            callEndpoint(user1Token, skipTokenCheck);
            timeList.add(System.currentTimeMillis() - time);
        }

        return timeList.stream()
                .mapToLong(it -> it)
                .summaryStatistics();
    }

    private String callEndpoint(String token, boolean skipTokenCheck) {
        return this.restTemplate.getForObject("http://localhost:" + port + "/sayHi?token=" + token
                        + "&skipTokenCheck=" + skipTokenCheck, String.class);
    }
}
