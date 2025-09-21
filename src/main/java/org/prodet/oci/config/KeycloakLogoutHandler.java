package org.prodet.oci.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class KeycloakLogoutHandler implements LogoutSuccessHandler {
    private static final String KEYCLOAK_LOGOUT_URL = "https://kc.prodet.org/realms/ociprodet/protocol/openid-connect/logout";
    private static final String POST_LOGOUT_REDIRECT_URI = "http://localhost:8080/"; // Change to your app's base URL

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String redirectUri = URLEncoder.encode(POST_LOGOUT_REDIRECT_URI, StandardCharsets.UTF_8);
        String logoutUrl = KEYCLOAK_LOGOUT_URL + "?post_logout_redirect_uri=" + redirectUri;
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.sendRedirect(logoutUrl);
    }
}

