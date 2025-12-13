package org.prodet.oci.config;

import jakarta.servlet.Servlet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "docker"})
public class H2ConsoleConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    public ServletRegistrationBean<Servlet> h2ConsoleServlet(
        @Value("${spring.h2.console.path:/h2-console}") String h2ConsolePath,
        @Value("${spring.h2.console.settings.web-allow-others:false}") boolean webAllowOthers
    ) {
        String mapping = h2ConsolePath.endsWith("/") ? h2ConsolePath + "*" : h2ConsolePath + "/*";
        var registration = new ServletRegistrationBean<>(instantiateH2Servlet(), mapping);
        registration.addInitParameter("webAllowOthers", Boolean.toString(webAllowOthers));
        return registration;
    }

    private static Servlet instantiateH2Servlet() {
        for (String className : List.of("org.h2.server.web.JakartaWebServlet", "org.h2.server.web.WebServlet")) {
            try {
                return (Servlet) Class.forName(className).getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException ignored) {
                // try next
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to instantiate " + className, e);
            }
        }
        throw new IllegalStateException("H2 console enabled, but no H2 WebServlet found on the classpath.");
    }
}
