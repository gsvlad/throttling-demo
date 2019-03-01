package demo.throttling.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class DefaultSlaService implements SlaService {

    private static final String USER_1 = "John";
    private static final String USER_2 = "Bob";

    private static final Map<String, SLA> userToSLA = new HashMap<String, SLA>() {{
        put(USER_1, new SLA(USER_1, 3));
        put(USER_2, new SLA(USER_2, 5));
    }};

    private static final Map<String, String> tokenToUser = new HashMap<String, String>() {{
        put("johnToken1", USER_1);
        put("johnToken2", USER_1);
        put("johnToken3", USER_1);
        put("bobToken1", USER_2);
        put("bobToken2", USER_2);
    }};

    @Override
    public CompletableFuture<SLA> getSlaByToken(String token) {
        return CompletableFuture.supplyAsync(() -> {
            hardWork();

            return Optional.ofNullable(tokenToUser.get(token))
                    .map(userToSLA::get)
                    .orElse(GUEST_SLA);
        });
    }

    private void hardWork() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // never mind
        }
    }
}