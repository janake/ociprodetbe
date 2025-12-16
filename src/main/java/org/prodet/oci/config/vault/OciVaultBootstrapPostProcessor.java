package org.prodet.oci.config.vault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Applies {@link OciVaultEnvironmentPostProcessor} as a fallback during the Spring bean factory phase.
 * <p>
 * This runs before datasource/flyway beans are created, so it can still inject passwords from OCI Vault.
 */
public class OciVaultBootstrapPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private static final Logger log = LoggerFactory.getLogger(OciVaultBootstrapPostProcessor.class);

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            this.environment = configurableEnvironment;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (environment == null) {
            return;
        }

        // If the EnvironmentPostProcessor already ran, it will have created this property source.
        if (environment.getPropertySources().contains("ociVaultSecrets")) {
            log.debug("OCI Vault secrets already applied (property source present).");
            return;
        }

        // Delegate to the same logic (adds property source if enabled).
        new OciVaultEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
    }
}

