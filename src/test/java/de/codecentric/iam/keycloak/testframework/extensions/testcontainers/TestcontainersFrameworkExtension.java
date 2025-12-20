package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import com.google.auto.service.AutoService;
import org.keycloak.testframework.CoreTestFrameworkExtension;
import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.admin.AdminClientSupplier;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.KeycloakServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entrypoint for our extension of Keycloak's Test Framework to integrate with Keycloak's Testcontainers Module.
 */
@AutoService(TestFrameworkExtension.class)
public class TestcontainersFrameworkExtension extends CoreTestFrameworkExtension {
    @Override
    public List<Supplier<?, ?>> suppliers() {
        var suppliers = new ArrayList<>(super.suppliers());

        // Supplier for Keycloak instances running in testcontainers
        suppliers.add(new TestcontainersKeycloakServerSupplier());

        // Replace Admin Client supplier with a version that is able to supply a Keycloak Testcontainers Admin Client
        // when being asked for
        suppliers.removeIf(AdminClientSupplier.class::isInstance);
        suppliers.add(new TestcontainersKeycloakAdminClientSupplier());

        return suppliers;
    }

    @Override
    public Map<Class<?>, String> valueTypeAliases() {
        return Map.of(KeycloakServer.class, "server");
    }
}
