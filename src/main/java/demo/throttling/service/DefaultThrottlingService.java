package demo.throttling.service;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static demo.throttling.service.SlaService.GUEST_SLA;

@Service
public class DefaultThrottlingService implements ThrottlingService {

    private final SlaService slaService;
    private ConcurrentHashMap<String, SlaService.SLA> tokenToSLA = new ConcurrentHashMap<>();

    // Time of last N calls per user. N = userRPS
    private ConcurrentHashMap<String, CircularFifoQueue<Long>> userRequests = new ConcurrentHashMap<>();

    // possible improvement: periodically clean concurrentMaps in order to minimize memory footprint.
    // For example, each hour remove users and their tokens, where last request older than hour.
    // Last request is taken from userRequests, user-tokens relationship could be cached in another map.

    @Autowired
    public DefaultThrottlingService(SlaService slaService) {
        this.slaService = slaService;
    }

    @Override
    public boolean isRequestAllowed(Optional<String> token) {
        long now = System.currentTimeMillis();

        SlaService.SLA sla = token.map(this::getSla)
                .orElse(GUEST_SLA);

        String user = sla.getUser();

        MutableBoolean allow = new MutableBoolean();

        // atomic per user
        userRequests.compute(user, (u, queue) -> {
            if (queue == null) {
                queue = new CircularFifoQueue<>(sla.getRps());
            }
            if (queue.isAtFullCapacity() && !queue.isEmpty() && (now - queue.peek() < 1000)) {
                allow.value = false;
            } else {
                queue.add(now);
            }
            return queue;
        });

        return allow.value;
    }

    private SlaService.SLA getSla(String token) {
        // atomic per token
        return tokenToSLA.computeIfAbsent(token, t -> {
            slaService.getSlaByToken(t)
                    .thenAccept(sla -> {
                        tokenToSLA.put(t, sla);
                    });

            return GUEST_SLA;
        });
    }

    private class MutableBoolean {
        private boolean value = true;
    }
}
