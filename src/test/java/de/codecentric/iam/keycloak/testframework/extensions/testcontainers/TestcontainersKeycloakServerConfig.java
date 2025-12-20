package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;

/**
 * Implementation of {@link org.keycloak.testframework.server.DefaultKeycloakServerConfig} that supports the
 * execution of Keycloak testcontainers configured by {@link TestcontainersKeycloakServerConfigBuilder}s.
 */
public abstract class TestcontainersKeycloakServerConfig extends DefaultKeycloakServerConfig {
    public abstract TestcontainersKeycloakServerConfigBuilder getBuilder();

    final KeycloakContainer startContainer() {
        var container = getBuilder().getConfiguredKeycloakContainer();
        container.start();
        return container;
    }
}
