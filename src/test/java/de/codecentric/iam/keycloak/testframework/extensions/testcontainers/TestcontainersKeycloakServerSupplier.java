package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.server.KeycloakServer;
import org.keycloak.testframework.server.RemoteKeycloakServer;

/**
 * Keycloak Test Framework {@link Supplier} for Keycloak instances running in testcontainers.
 */
public class TestcontainersKeycloakServerSupplier implements Supplier<KeycloakServer, KeycloakIntegrationTest> {
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
        var annotation = instanceContext.getAnnotation();
        var containerConfig = (TestcontainersKeycloakServerConfig) SupplierHelpers.getInstance(annotation.config());
        var container = containerConfig.startContainer();
        return new TestcontainersKeycloakServer(container.getAuthServerUrl(), container.getKeycloakAdminClient());
    }

    static class TestcontainersKeycloakServer extends RemoteKeycloakServer {
        private final String baseUrl;
        private final Keycloak adminClient;

        public TestcontainersKeycloakServer(String baseUrl, Keycloak adminClient) {
            this.baseUrl = baseUrl;
            this.adminClient = adminClient;
        }

        @Override
        public String getBaseUrl() {
            return baseUrl;
        }

        public Keycloak getAdminClient() {
            return adminClient;
        }
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
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
    public void close(InstanceContext<KeycloakServer, KeycloakIntegrationTest> instanceContext) {
        instanceContext.getValue().stop();
    }

    @Override
    public int order() {
        return SupplierOrder.KEYCLOAK_SERVER;
    }
}
