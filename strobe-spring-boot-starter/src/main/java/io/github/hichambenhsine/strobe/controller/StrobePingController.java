package io.github.hichambenhsine.strobe.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health and connectivity probe endpoint for Strobe.
 * Used to verify that the Strobe starter has been properly bootstrapped
 * and can serve HTTP requests within the host application.
 */
@RestController
@RequestMapping("/strobe/api")
public class StrobePingController {

    /**
     * Returns a simple status payload to confirm the starter is active.
     *
     * @return Map containing operational status and verification message.
     */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "status", "UP",
                "message", "Strobe Starter is active and running!"
        );
    }
}
