package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@SpringBootApplication
public class SpringBootDockerExampleApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootDockerExampleApp.class, args);
    }
}

@RestController
class WebController {

    @GetMapping
    public String home() {
        return "Hello Docker World";
    }
}
