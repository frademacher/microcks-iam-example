package de.codecentric.iam.keycloak.testframework.extensions.testcontainers.annotations;

import de.codecentric.iam.keycloak.testframework.extensions.testcontainers.TestcontainersKeycloakServerConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithKeycloakTestcontainer {
    Class<? extends TestcontainersKeycloakServerConfig> config();
}