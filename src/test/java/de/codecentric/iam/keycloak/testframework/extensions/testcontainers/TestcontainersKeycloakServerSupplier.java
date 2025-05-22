package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import de.codecentric.iam.keycloak.testframework.extensions.testcontainers.annotations.WithKeycloakTestcontainer;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.server.KeycloakServer;
import org.keycloak.testframework.server.RemoteKeycloakServer;

/**
 * Keycloak Test Framework {@link Supplier} for the {@link WithKeycloakTestcontainer} annotation.
 */
public class TestcontainersKeycloakServerSupplier implements Supplier<KeycloakServer, WithKeycloakTestcontainer> {
    @Override
    public Class<KeycloakServer> getValueType() {
        return KeycloakServer.class;
    }

    @Override
    public Class<WithKeycloakTestcontainer> getAnnotationClass() {
        return WithKeycloakTestcontainer.class;
    }

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
    public KeycloakServer getValue(
        InstanceContext<KeycloakServer, WithKeycloakTestcontainer> instanceContext
    ) {
        var annotation = instanceContext.getAnnotation();
        var testcontainersConfig = SupplierHelpers.getInstance(annotation.config());
        var container = testcontainersConfig.configure().startContainer();
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
    public boolean compatible(InstanceContext<KeycloakServer, WithKeycloakTestcontainer> a,
        RequestedInstance<KeycloakServer, WithKeycloakTestcontainer> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(InstanceContext<KeycloakServer, WithKeycloakTestcontainer> instanceContext) {
        instanceContext.getValue().stop();
    }

    @Override
    public int order() {
        return SupplierOrder.KEYCLOAK_SERVER;
    }
}
