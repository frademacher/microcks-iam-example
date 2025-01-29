package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

/**
 * Configuration interface for the {@link TestcontainersKeycloakServerSupplier}.
 */
public interface TestcontainersKeycloakServerConfig {
    TestcontainersKeycloakServerConfigBuilder configure();
}
