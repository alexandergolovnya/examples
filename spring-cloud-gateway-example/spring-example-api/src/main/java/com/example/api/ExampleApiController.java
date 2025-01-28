package com.example.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/1.0/example")
public class ExampleApiController {

    @GetMapping
    public String getExample() throws InterruptedException {
        int timeout = 1000;
        Thread.sleep(timeout);
        String message = "Example GET API response after waiting for " + timeout + "ms";
        log.info(message);
        return message;
    }
}
