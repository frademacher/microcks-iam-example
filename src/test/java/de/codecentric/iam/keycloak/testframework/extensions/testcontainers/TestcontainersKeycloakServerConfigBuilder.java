package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.keycloak.testframework.config.Config;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.Properties;

/**
 * Builder for the configurations of {@link org.keycloak.testframework.server.KeycloakServer}s that run inside a
 * Keycloak testcontainer. Basically, the builder methods wrap those of
 * {@link dasniko.testcontainers.keycloak.ExtendableKeycloakContainer}.
 * @see TestcontainersKeycloakServerSupplier
 */
public class TestcontainersKeycloakServerConfigBuilder {
    private final KeycloakContainer keycloakContainer;

    public TestcontainersKeycloakServerConfigBuilder() {
        try (var is = TestcontainersKeycloakServerConfigBuilder.class.getResourceAsStream("/application.properties")) {
            var props = new Properties();
            props.load(is);
            var keycloakVersion = props.getProperty("keycloak.version");
            keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:" + keycloakVersion);
            keycloakContainer
                .withAdminUsername(Config.getAdminUsername())
                .withAdminPassword(Config.getAdminPassword());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public KeycloakContainer getConfiguredKeycloakContainer() {
        return keycloakContainer;
    }

    public TestcontainersKeycloakServerConfigBuilder withCopyClasspathResourceToContainer(String resourcePath,
        String containerPath) {
        keycloakContainer.withCopyFileToContainer(MountableFile.forClasspathResource(resourcePath),
            containerPath);
        return this;
    }

    public TestcontainersKeycloakServerConfigBuilder withWriteStringToContainerFile(String s, String containerPath) {
        keycloakContainer.withCopyToContainer(Transferable.of(s), containerPath);
        return this;
    }

    public TestcontainersKeycloakServerConfigBuilder withNetwork(Network network) {
        keycloakContainer.withNetwork(network);
        return this;
    }

    public TestcontainersKeycloakServerConfigBuilder withDebugFixedPort(int hostPort, boolean suspend) {
        keycloakContainer.withDebugFixedPort(hostPort, suspend);
        return this;
    }

    public TestcontainersKeycloakServerConfigBuilder withProviderClassesFrom(String... classesResourcePaths) {
        keycloakContainer.withProviderClassesFrom(classesResourcePaths);
        return this;
    }

    public TestcontainersKeycloakServerConfigBuilder withRealmImportFile(String importFile) {
        keycloakContainer.withRealmImportFile(importFile);
        return this;
    }
}
