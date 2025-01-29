package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.admin.KeycloakAdminClientSupplier;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.KeycloakServer;

/**
 * Keycloak Test Framework {@link Supplier} for the {@link InjectAdminClient} annotation capable of returning a Keycloak
 * Testcontainers Admin Client when being asked for.
 */
public class TestcontainersKeycloakAdminClientSupplier extends KeycloakAdminClientSupplier {
    @Override
    public Keycloak getValue(InstanceContext<Keycloak, InjectAdminClient> instanceContext) {
        var server = instanceContext.getDependency(KeycloakServer.class);
        if (server instanceof TestcontainersKeycloakServerSupplier.TestcontainersKeycloakServer tks)
            return tks.getAdminClient();

        return super.getValue(instanceContext);
    }
}
