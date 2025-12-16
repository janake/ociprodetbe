package org.prodet.oci.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fail fast in prod if DB credentials/connectivity are broken.
 * This avoids only discovering issues on the first API call.
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class DbStartupCheckConfig {

    @Bean
    ApplicationRunner dbStartupCheck(JdbcTemplate jdbcTemplate) {
        return args -> jdbcTemplate.queryForObject("SELECT 1 FROM dual", Integer.class);
    }
}

