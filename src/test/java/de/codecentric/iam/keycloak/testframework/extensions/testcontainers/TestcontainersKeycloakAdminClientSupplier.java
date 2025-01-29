package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.admin.KeycloakAdminClientSupplier;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.server.KeycloakServer;

public class TestcontainersKeycloakAdminClientSupplier extends KeycloakAdminClientSupplier {
    @Override
    public Keycloak getValue(InstanceContext<Keycloak, InjectAdminClient> instanceContext) {
        var server = instanceContext.getDependency(KeycloakServer.class);
        if (server instanceof TestcontainersKeycloakServerSupplier.TestcontainersKeycloakServer tks)
            return tks.getAdminClient();

        return super.getValue(instanceContext);
    }
}
