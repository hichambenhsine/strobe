package io.github.hichambenhsine.strobe.config;


import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main auto-configuration entry point for the Strobe starter.
 * Automatically scans and registers all Strobe components (controllers, services, configurations)
 * into the host Spring Boot application context.
 */
@AutoConfiguration
@ComponentScan(basePackages = "io.github.hichambenhsine.strobe")
public class StrobeAutoConfiguration {
}
