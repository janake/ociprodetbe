package org.prodet.oci.config.vault;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Fallback bootstrap hook for OCI Vault secrets.
 * <p>
 * Some container/buildpack layouts can make {@link org.springframework.boot.env.EnvironmentPostProcessor}
 * registration hard to verify at runtime. This BeanFactoryPostProcessor ensures Vault secrets are applied
 * before datasource/flyway beans are instantiated.
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class OciVaultBootstrapConfiguration {

    @Bean
    static OciVaultBootstrapPostProcessor ociVaultBootstrapPostProcessor() {
        return new OciVaultBootstrapPostProcessor();
    }
}

