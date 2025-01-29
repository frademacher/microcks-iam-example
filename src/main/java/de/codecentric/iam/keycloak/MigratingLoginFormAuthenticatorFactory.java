package de.codecentric.iam.keycloak;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.KeycloakSession;

@AutoService(AuthenticatorFactory.class)
public class MigratingLoginFormAuthenticatorFactory extends UsernamePasswordFormFactory {
    private static final String PROVIDER_ID = "migrating-login-form-factory";
    private static final UsernamePasswordForm SINGLETON = new MigratingLoginFormAuthenticator();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Migrating Login Form";
    }

    @Override
    public String getHelpText() {
        return "Controller for the migrating login form.";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }
}
