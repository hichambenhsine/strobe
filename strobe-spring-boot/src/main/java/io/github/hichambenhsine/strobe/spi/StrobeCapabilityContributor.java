package io.github.hichambenhsine.strobe.spi;

/**
 * Service Provider Interface (SPI) enabling independent Strobe modules to register runtime capabilities.
 * Enforces the Open/Closed Principle by decoupling capability discovery from StrobeMetadataService.
 */
public interface StrobeCapabilityContributor {

    /**
     * Unique capability key exposed in the metadata payload (e.g., "actuator", "gateway").
     *
     * @return Capability identifier key.
     */
    String getCapabilityName();

    /**
     * Strongly-typed capability status contract serialized into the capabilities map.
     *
     * @return Capability status object.
     */
    Object getCapabilityStatus();
}