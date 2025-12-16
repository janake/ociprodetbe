package org.prodet.oci.config.vault;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import com.oracle.bmc.secrets.responses.GetSecretBundleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads DB credentials from OCI Vault (Secrets) during bootstrap.
 * <p>
 * Enables keeping DB passwords out of env vars (use Secret OCIDs instead).
 */
public class OciVaultEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE_NAME = "ociVaultSecrets";
    private static final Logger log = LoggerFactory.getLogger(OciVaultEnvironmentPostProcessor.class);

    @Override
    public int getOrder() {
        // Run after config data (so application-prod.properties is already loaded)
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isEnabled(environment)) {
            return;
        }

        Map<String, Object> overrides = new HashMap<>();

        String dbAppUser = environment.getProperty("spring.datasource.username");
        String flywayUser = environment.getProperty("spring.flyway.user");

        String regionId = require(environment, "oci.vault.region");

        String authMode = environment.getProperty("oci.vault.auth", "instance_principal");
        boolean hasAppUserSecret = !isBlank(environment.getProperty("oci.vault.secrets.db-app-user"));
        boolean hasAppPasswordSecret = !isBlank(environment.getProperty("oci.vault.secrets.db-app-password"));
        boolean hasOwnerUserSecret = !isBlank(environment.getProperty("oci.vault.secrets.db-owner-user"));
        boolean hasOwnerPasswordSecret = !isBlank(environment.getProperty("oci.vault.secrets.db-owner-password"));

        log.info("OCI Vault enabled (auth={}, region={})", authMode, regionId);

        try (SecretsClient client = createClient(environment, regionId)) {
            // Datasource password must always come from Vault when enabled.
            String datasourcePassword = readSecret(environment, client, "oci.vault.secrets.db-app-password");
            overrides.put("spring.datasource.password", datasourcePassword);

            // If a username Secret OCID is provided, prefer it as the source of truth (even though username is not sensitive).
            // This avoids env/secret mismatches (e.g., DB_APP_USER != secret value) which otherwise results in ORA-01017.
            if (hasAppUserSecret) {
                String secretUser = readSecret(environment, client, "oci.vault.secrets.db-app-user");
                overrides.put("spring.datasource.username", secretUser);
                if (!isBlank(dbAppUser) && !dbAppUser.equals(secretUser)) {
                    log.warn("DB_APP_USER differs from Vault username; using Vault value.");
                }
            }

            String effectiveDatasourceUser = String.valueOf(overrides.getOrDefault("spring.datasource.username", dbAppUser));
            if (isBlank(effectiveDatasourceUser)) {
                throw new IllegalStateException("Datasource username is missing; set DB_APP_USER or oci.vault.secrets.db-app-user.");
            }

            if (hasOwnerUserSecret && !hasOwnerPasswordSecret) {
                throw new IllegalStateException("oci.vault.secrets.db-owner-user is set but oci.vault.secrets.db-owner-password is missing.");
            }

            if (hasOwnerPasswordSecret) {
                if (hasOwnerUserSecret) {
                    overrides.put("spring.flyway.user", readSecret(environment, client, "oci.vault.secrets.db-owner-user"));
                } else if (isBlank(flywayUser)) {
                    throw new IllegalStateException("Flyway user is missing; set DB_OWNER_USER (spring.flyway.user) or oci.vault.secrets.db-owner-user.");
                }
                overrides.put("spring.flyway.password", readSecret(environment, client, "oci.vault.secrets.db-owner-password"));
            } else {
                // If no separate owner credentials are provided, always run Flyway with the datasource credentials.
                // If a different Flyway user is configured (e.g. DB_OWNER_USER=app_owner), fail fast with a clear message.
                if (!isBlank(flywayUser) && !flywayUser.equals(effectiveDatasourceUser)) {
                    throw new IllegalStateException(
                        "spring.flyway.user is set to '" + flywayUser + "' but no owner password secret is configured. " +
                            "Either set OCI_SECRET_DB_OWNER_PASSWORD or set DB_OWNER_USER=DB_APP_USER."
                    );
                }
                overrides.put("spring.flyway.user", effectiveDatasourceUser);
                overrides.put("spring.flyway.password", datasourcePassword);
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                "OCI Vault is enabled but failed to load DB credentials from Secrets. " +
                    "Check OCI dynamic-group/policy permissions, secret OCIDs, region, and instance-principal metadata access.",
                e
            );
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, overrides));
        }
    }

    private static SecretsClient createClient(ConfigurableEnvironment environment, String regionId) {
        String authMode = environment.getProperty("oci.vault.auth", "instance_principal").toLowerCase(Locale.ROOT).trim();

        AbstractAuthenticationDetailsProvider provider;
        try {
            provider = switch (authMode) {
                case "instance_principal", "instance", "instance_principals" ->
                    InstancePrincipalsAuthenticationDetailsProvider.builder().build();
                case "resource_principal", "resource", "resource_principals" ->
                    ResourcePrincipalAuthenticationDetailsProvider.builder().build();
                case "config_file", "config" -> {
                    String configPath = expandHome(environment.getProperty("oci.vault.oci-config-file", "~/.oci/config"));
                    String profile = environment.getProperty("oci.vault.oci-profile", "DEFAULT");
                    ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(configPath, profile);
                    yield new ConfigFileAuthenticationDetailsProvider(configFile);
                }
                default -> throw new IllegalArgumentException("Unsupported oci.vault.auth: " + authMode);
            };
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create OCI auth provider for oci.vault.auth=" + authMode, e);
        }

        SecretsClient client = SecretsClient.builder().build(provider);
        client.setRegion(Region.fromRegionId(regionId));
        return client;
    }

    private static String readSecret(ConfigurableEnvironment environment, SecretsClient client, String secretIdProperty) {
        String secretId = require(environment, secretIdProperty);
        GetSecretBundleResponse resp = client.getSecretBundle(
            GetSecretBundleRequest.builder()
                .secretId(secretId)
                .stage(GetSecretBundleRequest.Stage.Current)
                .build()
        );
        Base64SecretBundleContentDetails content = (Base64SecretBundleContentDetails) resp.getSecretBundle().getSecretBundleContent();
        byte[] decoded = java.util.Base64.getDecoder().decode(content.getContent());
        return new String(decoded, StandardCharsets.UTF_8).trim();
    }

    private static String expandHome(String path) {
        if (path == null) return null;
        if (path.equals("~")) return System.getProperty("user.home");
        if (path.startsWith("~/")) return System.getProperty("user.home") + path.substring(1);
        return path;
    }

    private static boolean isEnabled(ConfigurableEnvironment env) {
        boolean enabled = Boolean.parseBoolean(env.getProperty("oci.vault.enabled", "false"));
        if (!enabled) return false;

        // Only enforce for prod by default; if you want it for other profiles, set oci.vault.allow-nonprod=true
        boolean allowNonProd = Boolean.parseBoolean(env.getProperty("oci.vault.allow-nonprod", "false"));
        if (allowNonProd) return true;

        // Spring may not have activated profiles yet at this phase, so check both the parsed active profiles and the property.
        String activeProfilesProp = env.getProperty("spring.profiles.active", "");
        if (activeProfilesProp.contains("prod")) return true;

        for (String profile : env.getActiveProfiles()) {
            if ("prod".equals(profile)) return true;
        }
        log.debug(
            "OCI Vault is enabled but not applied because profile is not prod (spring.profiles.active='{}', activeProfiles={}); set oci.vault.allow-nonprod=true to force it.",
            activeProfilesProp,
            String.join(",", env.getActiveProfiles())
        );
        return false;
    }

    private static String require(ConfigurableEnvironment env, String key) {
        String value = env.getProperty(key);
        if (isBlank(value)) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
