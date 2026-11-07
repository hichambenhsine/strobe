package io.github.hichambenhsine.strobe.model;

/**
 * Data transfer object encapsulating the baseline runtime environment metadata.
 */
public record StrobeMetadataDto(String applicationName, StrobeActuatorStatus actuator) {}
