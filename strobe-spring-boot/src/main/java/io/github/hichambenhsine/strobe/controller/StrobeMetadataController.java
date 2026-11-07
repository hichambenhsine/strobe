package io.github.hichambenhsine.strobe.controller;

import io.github.hichambenhsine.strobe.model.StrobeMetadataDto;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint exposing host application context metadata.
 * Instantiated strictly via StrobeAutoConfiguration.
 */
@RestController
@RequestMapping("/strobe/api")
public class StrobeMetadataController {

    private final Environment environment;

    public StrobeMetadataController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/metadata")
    public StrobeMetadataDto getMetadata() {
        return new StrobeMetadataDto(environment.getProperty("spring.application.name", "spring-boot-app"));
    }
}