package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.server.AbstractKeycloakServerSupplier;
import org.keycloak.testframework.server.KeycloakServer;
import org.keycloak.testframework.server.RemoteKeycloakServer;

/**
 * Keycloak Test Framework {@link Supplier} for Keycloak instances running in testcontainers.
 */
public class TestcontainersKeycloakServerSupplier extends AbstractKeycloakServerSupplier {
    @Override
    public String getAlias() {
        return "testcontainers";
    }

    /**
     * Fire up a new Keycloak testcontainer with the {@link TestcontainersKeycloakServerConfig} provided with the
     * annotation. After successful firing up, return the Testcontainers-specific {@link KeycloakServer} implementation
     * {@link TestcontainersKeycloakServer}.
     */
    @Override
    public KeycloakServer getValue(InstanceContext<KeycloakServer, KeycloakIntegrationTest> instanceContext) {
        var server = (TestcontainersKeycloakServer) getServer();
        server.startContainer(
            (TestcontainersKeycloakServerConfig) SupplierHelpers.getInstance(instanceContext.getAnnotation().config())
        );
        return server;
    }

    @Override
    public KeycloakServer getServer() {
        return new TestcontainersKeycloakServer();
    }

    static class TestcontainersKeycloakServer extends RemoteKeycloakServer {
        private KeycloakContainer container;

        public void startContainer(TestcontainersKeycloakServerConfig config) {
            container = config.getBuilder().getConfiguredKeycloakContainer();
            container.start();
        }

        @Override
        public String getBaseUrl() {
            return container.getAuthServerUrl();
        }

        public Keycloak getAdminClient() {
            return container.getKeycloakAdminClient();
        }
    }

    @Override
    public boolean compatible(InstanceContext<KeycloakServer, KeycloakIntegrationTest> a,
        RequestedInstance<KeycloakServer, KeycloakIntegrationTest> b) {
        var aConfigClass = a.getAnnotation().config();
        var bConfigClass = b.getAnnotation().config();
        return aConfigClass.equals(bConfigClass) &&
            TestcontainersKeycloakServerConfig.class.isAssignableFrom(aConfigClass) &&
            TestcontainersKeycloakServerConfig.class.isAssignableFrom(bConfigClass);
    }

    @Override
    public boolean requiresDatabase() {
        return false;
    }

    @Override
    public Logger getLogger() {
        return null;
    }
}
