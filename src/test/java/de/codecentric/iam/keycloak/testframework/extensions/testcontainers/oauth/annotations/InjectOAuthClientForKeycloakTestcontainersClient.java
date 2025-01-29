package de.codecentric.iam.keycloak.testframework.extensions.testcontainers.oauth.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inject an OAuth Client for a certain Keycloak client running inside a Keycloak testcontainer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectOAuthClientForKeycloakTestcontainersClient {
    String clientId();
}