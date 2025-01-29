package de.codecentric.iam.keycloak.testframework.extensions.testcontainers.oauth;

import de.codecentric.iam.keycloak.testframework.extensions.testcontainers.oauth.annotations.InjectOAuthClientForKeycloakTestcontainersClient;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.realm.ManagedRealm;

public class OAuthClientForKeycloakTestcontainersClientSupplier
    implements Supplier<OAuthClientForKeycloakTestcontainersClient, InjectOAuthClientForKeycloakTestcontainersClient> {
    @Override
    public Class<InjectOAuthClientForKeycloakTestcontainersClient> getAnnotationClass() {
        return InjectOAuthClientForKeycloakTestcontainersClient.class;
    }

    @Override
    public Class<OAuthClientForKeycloakTestcontainersClient> getValueType() {
        return OAuthClientForKeycloakTestcontainersClient.class;
    }

    @Override
    public OAuthClientForKeycloakTestcontainersClient getValue(
        InstanceContext<
            OAuthClientForKeycloakTestcontainersClient,
            InjectOAuthClientForKeycloakTestcontainersClient
        > instanceContext
    ) {
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class);
        var clientId = instanceContext.getAnnotation().clientId();
        return new OAuthClientForKeycloakTestcontainersClient(realm, clientId);
    }

    @Override
    public boolean compatible(
        InstanceContext<OAuthClientForKeycloakTestcontainersClient, InjectOAuthClientForKeycloakTestcontainersClient> a,
        RequestedInstance<
            OAuthClientForKeycloakTestcontainersClient,
            InjectOAuthClientForKeycloakTestcontainersClient
        > b
    ) {
        return true;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public void close(
        InstanceContext<
            OAuthClientForKeycloakTestcontainersClient,
            InjectOAuthClientForKeycloakTestcontainersClient
        > instanceContext
    ) {
        instanceContext.getValue().close();
    }
}