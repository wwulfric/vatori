package dev.vatori.desktop.flow;

import com.vaadin.flow.spring.VaadinScopesConfig;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcSpringApplication {
    private static final String EXCLUDED_VAADIN_WEB_AUTO_CONFIGURATIONS = String.join(",",
            "com.vaadin.flow.spring.SpringBootAutoConfiguration",
            "com.vaadin.flow.spring.SpringSecurityAutoConfiguration");

    private FlowIpcSpringApplication() {
    }

    static ConfigurableApplicationContext start(Class<?> applicationClass, String[] args) {
        return new SpringApplicationBuilder(applicationClass, VaadinScopesConfig.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .properties(defaultProperties())
                .run(args);
    }

    private static Map<String, Object> defaultProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.main.web-application-type", "none");
        properties.put("spring.autoconfigure.exclude", EXCLUDED_VAADIN_WEB_AUTO_CONFIGURATIONS);
        properties.put("vaadin.desktop.compatibility-mode", "false");
        properties.put("vaadin.launch-browser", "false");
        return properties;
    }
}
