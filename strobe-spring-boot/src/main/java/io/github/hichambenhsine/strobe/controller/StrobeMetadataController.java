package io.github.hichambenhsine.strobe.controller;

import io.github.hichambenhsine.strobe.model.StrobeActuatorStatus;
import io.github.hichambenhsine.strobe.model.StrobeMetadataDto;
import io.github.hichambenhsine.strobe.service.StrobeMetadataService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
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

    private final StrobeMetadataService strobeMetadataService;

    public StrobeMetadataController(StrobeMetadataService strobeMetadataService) {
        this.strobeMetadataService = strobeMetadataService;
    }


    @GetMapping("/metadata")
    public StrobeMetadataDto getMetadata() {
        return strobeMetadataService.getMetadata();
    }
}