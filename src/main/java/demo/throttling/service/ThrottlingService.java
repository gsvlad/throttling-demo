package demo.throttling.service;

import java.util.Optional;

public interface ThrottlingService {

    boolean isRequestAllowed(Optional<String> token);
}
