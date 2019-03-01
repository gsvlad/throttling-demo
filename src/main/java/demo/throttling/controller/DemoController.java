package demo.throttling.controller;

import demo.throttling.service.ThrottlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class DemoController {

    @Autowired
    private ThrottlingService throttlingService;

    @GetMapping("/sayHi")
    public String sayHi(@RequestParam(required = false) String token,
                        @RequestParam(defaultValue = "false") boolean skipTokenCheck) {

        if (skipTokenCheck || throttlingService.isRequestAllowed(Optional.ofNullable(token))) {
            return "Hello";
        } else {
            return "NO!";
        }
    }
}
