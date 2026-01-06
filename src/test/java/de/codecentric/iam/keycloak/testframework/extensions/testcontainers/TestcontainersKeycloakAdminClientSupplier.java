package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import com.google.common.collect.ImmutableList;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.admin.AdminClientSupplier;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.KeycloakServer;

import java.util.List;

/**
 * Keycloak Test Framework {@link Supplier} for the {@link InjectAdminClient} annotation capable of returning a Keycloak
 * Testcontainers Admin Client when being asked for.
 */
public class TestcontainersKeycloakAdminClientSupplier extends AdminClientSupplier {
    @Override
    public List<Dependency> getDependencies(RequestedInstance<Keycloak, InjectAdminClient> instanceContext) {
        return ImmutableList.<Dependency>builder()
            .addAll(super.getDependencies(instanceContext))
            // Signal testframework that this supplier also depends on a KeycloakServer instance for test execution
            // (see https://github.com/keycloak/keycloak/issues/44947)
            .addAll(DependenciesBuilder.create(AdminClientFactory.class).add(KeycloakServer.class).build())
            .build();
    }

    @Override
    public Keycloak getValue(InstanceContext<Keycloak, InjectAdminClient> instanceContext) {
        var server = instanceContext.getDependency(KeycloakServer.class);
        if (server instanceof TestcontainersKeycloakServerSupplier.TestcontainersKeycloakServer tks)
            return tks.getAdminClient();

        return super.getValue(instanceContext);
    }
}
