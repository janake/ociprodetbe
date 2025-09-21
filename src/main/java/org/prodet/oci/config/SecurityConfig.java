package org.prodet.oci.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final KeycloakLogoutHandler keycloakLogoutHandler;

    public SecurityConfig(KeycloakLogoutHandler keycloakLogoutHandler) {
        this.keycloakLogoutHandler = keycloakLogoutHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            // OAuth2 login requires HTTP session
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/", "/index.html",
                    "/actuator/health", "/actuator/info"
                ).permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            // Enable OAuth2 Login (authorization_code) for browser redirect to Keycloak
            .oauth2Login(oauth2 -> {})
            // Also accept Bearer JWTs for API clients
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
            // Redirect unauthenticated browser requests, but keep 401 for Bearer API calls
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(new BearerTokenAuthenticationEntryPoint(), new RequestHeaderRequestMatcher("Authorization"))
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak"))
                .defaultAccessDeniedHandlerFor(new BearerTokenAccessDeniedHandler(), new RequestHeaderRequestMatcher("Authorization"))
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(keycloakLogoutHandler)
            );

        return http.build();
    }
}
