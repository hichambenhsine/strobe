package io.github.hichambenhsine.strobe.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Diagnostic probe endpoint.
 * Instantiated strictly via StrobeAutoConfiguration.
 */
@RestController
@RequestMapping("/strobe/api")
public class StrobePingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "UP", "message", "Strobe Core Engine is active");
    }
}
