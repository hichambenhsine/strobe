package io.github.hichambenhsine.demoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Host demonstration application used to verify Strobe starter capabilities.
 * Simulates an external client project incorporating the Strobe dependency.
 */
@SpringBootApplication
public class DemoAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoAppApplication.class, args);
    }

}
