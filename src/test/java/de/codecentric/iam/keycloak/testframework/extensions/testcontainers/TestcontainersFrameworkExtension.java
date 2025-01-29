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

@AutoService(TestFrameworkExtension.class)
public class TestcontainersFrameworkExtension extends CoreTestFrameworkExtension {
    @Override
    public List<Supplier<?, ?>> suppliers() {
        var suppliers = new ArrayList<>(super.suppliers());

        suppliers.add(new TestcontainersKeycloakServerSupplier());
        suppliers.add(new OAuthClientForKeycloakTestcontainersClientSupplier());

        suppliers.removeIf(KeycloakAdminClientSupplier.class::isInstance);
        suppliers.add(new TestcontainersKeycloakAdminClientSupplier());

        return suppliers;
    }

    @Override
    public Map<Class<?>, String> valueTypeAliases() {
        return Map.of(KeycloakServer.class, "server");
    }
}
