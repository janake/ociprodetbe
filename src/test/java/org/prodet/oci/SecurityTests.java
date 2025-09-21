package org.prodet.oci;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiWithoutTokenShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/api/teszt").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
    }

    @Test
    void apiWithJwtShouldReturn200() throws Exception {
        SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor mockJwt = jwt()
                .jwt(jwt -> jwt
                        .claim("sub", "1234567890")
                        .claim("preferred_username", "tester")
                        .claim("scope", "openid profile")
                );

        mockMvc.perform(get("/api/teszt").with(mockJwt).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Hello").value("Bello"));
    }
}
