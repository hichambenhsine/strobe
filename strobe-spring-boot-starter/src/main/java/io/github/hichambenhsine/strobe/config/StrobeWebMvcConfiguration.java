package io.github.hichambenhsine.strobe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration responsible for serving the embedded Angular console.
 * Maps URL paths to static assets and configures SPA view forwardings.
 */
@Configuration
public class StrobeWebMvcConfiguration implements WebMvcConfigurer {

    /**
     * Maps the /strobe/** path pattern directly to the embedded static frontend assets.
     *
     * @param registry ResourceHandlerRegistry used to register asset handlers.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/strobe/**")
                .addResourceLocations("classpath:/static/strobe/");
    }

    /**
     * Forwards base path requests directly to the Angular application index.
     *
     * @param registry ViewControllerRegistry used to register view controllers.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/strobe")
                .setViewName("forward:/strobe/index.html");
        registry.addViewController("/strobe/")
                .setViewName("forward:/strobe/index.html");
    }
}
