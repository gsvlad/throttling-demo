package demo.throttling.service;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class DefaultThrottlingServiceTest {

    private String[] user1Tokens = new String[]{"johnToken1", "johnToken2", "johnToken3"};
    private String[] user2Tokens = new String[]{"bobToken1", "bobToken2"};
    private String[] unknownTokens = new String[]{"unknown1", "unknown2"};
    private Random random = new Random();

    private DefaultThrottlingService service;

    @Before
    public void setUp() {
        service = new DefaultThrottlingService(new DefaultSlaService());
    }

    @Test
    public void testOnCachedTokens() throws InterruptedException {
        // given
        // all tokens are cached
        Stream.of(user1Tokens)
                .forEach(this::askService);

        Thread.sleep(1000); // wait for cache

        // when
        int callsEachSecond = 10;
        int duringSeconds = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(callsEachSecond);
        AtomicInteger allowCount = new AtomicInteger();

        for (int sec = 0; sec < duringSeconds; sec++) {
            System.out.println((duringSeconds - sec) + " second(s) left");

            List<Callable<Void>> tasks = IntStream.range(0, callsEachSecond)
                    .mapToObj(i -> (Callable<Void>) () -> {
                        if (askService(randomToken(user1Tokens))) {
                            allowCount.incrementAndGet();
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

            executorService.invokeAll(tasks);
            Thread.sleep(1000);
        }

        // then
        // RPS for user1 = 3, so for 10 seconds we should have allowCount == 30
        assertEquals(duringSeconds * 3, allowCount.get());
    }

    @Test
    public void testOnCachedTokensWithOtherUsers() throws InterruptedException {
        // given
        // all tokens are cached
        Stream.of(user1Tokens)
                .forEach(this::askService);

        Thread.sleep(300); // wait for cache

        // when
        int callsEachSecond = 10;
        int duringSeconds = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(callsEachSecond);
        AtomicInteger allowCount = new AtomicInteger();

        for (int sec = 0; sec < duringSeconds; sec++) {
            System.out.println((duringSeconds - sec) + " second(s) left");

            List<Callable<Void>> tasks = IntStream.range(0, callsEachSecond)
                    .mapToObj(i -> (Callable<Void>) () -> {
                        askService(randomToken(user2Tokens));
                        askService(randomToken(unknownTokens));

                        if (askService(randomToken(user1Tokens))) {
                            allowCount.incrementAndGet();
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

            executorService.invokeAll(tasks);
            Thread.sleep(1000);
        }

        // then
        // RPS for user1 = 3, so for 10 seconds we should have allowCount == 30
        assertEquals(duringSeconds * 3, allowCount.get());
    }

    @Test
    public void testGuestUser() throws InterruptedException {
        // when
        int callsEachSecond = 15;
        ExecutorService executorService = Executors.newFixedThreadPool(callsEachSecond);
        AtomicInteger allowCount = new AtomicInteger();
        String[] allTokens = concat(user1Tokens, user2Tokens, unknownTokens);

        List<Callable<Void>> tasks = IntStream.range(0, callsEachSecond)
                .mapToObj(i -> (Callable<Void>) () -> {
                    if (askService(randomToken(allTokens))) {
                        allowCount.incrementAndGet();
                    }
                    return null;
                })
                .collect(Collectors.toList());

        executorService.invokeAll(tasks);

        // then
        // RPS for unauthorized user = 10
        assertEquals(10, allowCount.get());
    }

    private boolean askService(String s) {
        return service.isRequestAllowed(Optional.of(s));
    }

    private String randomToken(String[] tokens) {
        return tokens[random.nextInt(tokens.length)];
    }

    private String[] concat(String[]... arrays) {
        return Stream.of(arrays).flatMap(Stream::of).toArray(String[]::new);
    }
}