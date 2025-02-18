package com.example.demo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@TestConfiguration()
public class ExtraConfiguration {

    @RestController
    public static class ExtraController {
        @GetMapping("/added")
        public String added() {
            return "added";
        }
    }
}