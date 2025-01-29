package de.codecentric.iam.keycloak.testframework.extensions.testcontainers.annotations;

import de.codecentric.iam.keycloak.testframework.extensions.testcontainers.TestcontainersKeycloakServerConfig;
import de.codecentric.iam.keycloak.testframework.extensions.testcontainers.TestcontainersKeycloakServerSupplier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for our extension of Keycloak's Test Framework to integrate with Keycloak's Testcontainers Module. The
 * annotation signals the {@link TestcontainersKeycloakServerSupplier} that a Keycloak testcontainer shall be used for
 * tests with the given {@link TestcontainersKeycloakServerConfig}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithKeycloakTestcontainer {
    Class<? extends TestcontainersKeycloakServerConfig> config();
}