package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

public class TestcontainersKeycloakServerConfigBuilder {
    private final KeycloakContainer keycloakContainer;

    public TestcontainersKeycloakServerConfigBuilder() {
        this.keycloakContainer = new KeycloakContainer();
    }

    KeycloakContainer startContainer() {
        keycloakContainer.start();
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

    public TestcontainersKeycloakServerConfigBuilder withAdminCredentials(String username, String password) {
        keycloakContainer.withAdminUsername(username).withAdminPassword(password);
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
