package de.codecentric.iam.crm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmApiConfig {
    public static final String API_CONFIG_SECRET_NAME = "crm_api";

    private static final Logger logger = Logger.getLogger(CrmApiConfig.class);

    private static CrmApiConfigEntry cachedConfig;

    private CrmApiConfig() {
        // NOOP
    }

    public static CrmApiConfigEntry getConfigEntry(KeycloakSession session) {
        if (cachedConfig == null)
            cachedConfig = parseConfigEntry(session);
        return cachedConfig;
    }

    public static class CrmApiConfigEntry {
        @JsonProperty("url")
        private String url;
        @JsonProperty("api_token")
        private String apiToken;

        /**
         * Constructor for JSON deserialization
         */
        private CrmApiConfigEntry() {
            this(null, null);
        }

        public CrmApiConfigEntry(String url, String apiToken) {
            this.url = url;
            this.apiToken = apiToken;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
                this.url = url;
    }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }
    }

    private static CrmApiConfigEntry parseConfigEntry(KeycloakSession session) {
        var configJson = session.vault().getStringSecret(String.format("${vault.%s}", API_CONFIG_SECRET_NAME));
        try {
            return parseConfigEntry(configJson.get().orElse("{}"));
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }

    static CrmApiConfigEntry parseConfigEntry(String configJson) {
        if (StringUtils.isBlank(configJson))
            throw new IllegalArgumentException("No configuration for interaction with CRM API found. Probably it " +
                "isn't configured as the expected vault secret. Subsequent API calls are likely to fail.");

        try {
            return JsonSerialization.readValue(configJson, new TypeReference<>(){});
        } catch (IOException ex) {
            throw new IllegalArgumentException("Exception during parsing of the JSON configuration for interaction " +
                "with CRM API. Subsequent API calls are likely to fail.", ex);
        }
    }
}
