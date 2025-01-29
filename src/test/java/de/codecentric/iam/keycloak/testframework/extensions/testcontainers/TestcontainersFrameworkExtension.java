package de.codecentric.iam.keycloak.testframework.extensions.testcontainers;

import com.google.auto.service.AutoService;
import de.codecentric.iam.keycloak.testframework.extensions.testcontainers.oauth.OAuthClientForKeycloakTestcontainersClientSupplier;
import org.keycloak.testframework.CoreTestFrameworkExtension;
import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.admin.KeycloakAdminClientSupplier;
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

        // Suppliers for new annotations @WithKeycloakTestcontainer and
        // @InjectOAuthClientForKeycloakTestcontainersClient
        suppliers.add(new TestcontainersKeycloakServerSupplier());
        suppliers.add(new OAuthClientForKeycloakTestcontainersClientSupplier());

        // Replace Admin Client supplier with a version that is able to supply a Keycloak Testcontainers Admin Client
        // when being asked for
        suppliers.removeIf(KeycloakAdminClientSupplier.class::isInstance);
        suppliers.add(new TestcontainersKeycloakAdminClientSupplier());

        return suppliers;
    }

    @Override
    public Map<Class<?>, String> valueTypeAliases() {
        return Map.of(KeycloakServer.class, "server");
    }
}
