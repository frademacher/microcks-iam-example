package de.codecentric.iam.keycloak;

import de.codecentric.iam.crm.CrmApiFacade;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.PasswordCredentialProviderFactory;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

import static de.codecentric.iam.keycloak.UserAttributes.CRM_CUSTOMER_ADDRESS_ATTRIBUTE;
import static de.codecentric.iam.keycloak.UserAttributes.CRM_CUSTOMER_ID_ATTRIBUTE;
import static de.codecentric.iam.keycloak.UserUtils.updateKeycloakUser;
import static org.apache.http.HttpStatus.SC_OK;
import static org.keycloak.authentication.AuthenticationFlowError.INVALID_CREDENTIALS;
import static org.keycloak.models.UserModel.EMAIL;
import static org.keycloak.models.UserModel.FIRST_NAME;
import static org.keycloak.models.UserModel.LAST_NAME;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.services.managers.AuthenticationManager.FORM_USERNAME;
import static org.keycloak.userprofile.UserProfileContext.REGISTRATION;

/**
 * Implementation of Keycloak's {@link UsernamePasswordForm} authenticator which migrates an existing CRM customer into
 * a Keycloak user.
 */
public class MigratingLoginFormAuthenticator extends UsernamePasswordForm {
    private static final String CRM_LOGIN_TOKEN_JWT_AUTH_NOTE = "CRM_LOGIN_TOKEN_JWT_AUTH_NOTE";

    /**
     * Action implementation.
     */
    @Override
    public void action(AuthenticationFlowContext context) {
        var formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.cancelLogin();
            return;
        }

        var validForm = validateForm(context, formData);
        if (!validForm)
            return;

        /*
         * Acquire CRM customer login token set by form validation (see below) as a Keycloak auth note. Then, use the
         * token to extract CRM customer ID and address, which are to be stored as attributes for the new Keycloak user
         */
        var crmLoginTokenJwt = context.getAuthenticationSession().getAuthNote(CRM_LOGIN_TOKEN_JWT_AUTH_NOTE);
        if (StringUtils.isBlank(crmLoginTokenJwt)) {
            invalidCredentialsResponse(context);
            return;
        }
        context.getAuthenticationSession().removeAuthNote(CRM_LOGIN_TOKEN_JWT_AUTH_NOTE);
        AccessToken crmLoginToken;
        try {
            crmLoginToken = new JWSInput(crmLoginTokenJwt).readJsonContent(AccessToken.class);
        } catch (Exception ex) {
            invalidCredentialsResponse(context);
            return;
        }

        var crmCustomer = CrmApiFacade
            .session(context.getSession())
            .getCustomer(crmLoginTokenJwt);
        if (crmCustomer.isEmpty() || crmCustomer.get().httpStatus() != SC_OK) {
            invalidCredentialsResponse(context);
            return;
        }

        var username = formData.getFirst(FORM_USERNAME);
        // Retrieve existing Keycloak user or create new one
        var keycloakUser = Objects.requireNonNullElse(
                getKeycloakUser(context.getSession(), context.getRealm(), username),
                createKeycloakUser(context.getSession(), formData, crmCustomer.get().firstname(),
                    crmCustomer.get().lastname())
            );
        keycloakUser.setAttribute(CRM_CUSTOMER_ID_ATTRIBUTE, List.of(crmLoginToken.getSubject()));
        keycloakUser.setAttribute(CRM_CUSTOMER_ADDRESS_ATTRIBUTE, List.of(crmCustomer.get().address()));
        updateKeycloakUser(context.getSession(), keycloakUser);
        context.setUser(keycloakUser);
        context.success();
    }

    /**
     * Validate form inputs.
     */
    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        var username = formData.getFirst(FORM_USERNAME);
        var password = formData.getFirst(PASSWORD);
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password))
            return super.validateForm(context, formData);

        // Check whether the given Keycloak user credentials are also valid for the corresponding CRM customer
        var crmLoginResponse = CrmApiFacade
            .session(context.getSession())
            .login(username, password);
        if (crmLoginResponse.isEmpty() || crmLoginResponse.get().httpStatus() != SC_OK) {
            invalidCredentialsResponse(context);
            return false;
        }

        // Consider validation successful if a Keycloak user for the corresponding CRM customer doesn't exist yet.
        // Otherwise, let the superclass perform its validation logic on the existing Keycloak user, which comprises,
        // among others, checking the user's Keycloak credentials for correctness.
        var validForm = !existsKeycloakUser(context.getSession(), context.getRealm(), username) ||
            super.validateForm(context, formData);
        if (!validForm)
            return false;

        // Store CRM login token for reuse by form action (see above)
        context.getAuthenticationSession().setAuthNote(CRM_LOGIN_TOKEN_JWT_AUTH_NOTE,
            crmLoginResponse.get().loginToken());
        return true;
    }

    private void invalidCredentialsResponse(AuthenticationFlowContext context) {
        context.failureChallenge(INVALID_CREDENTIALS,
            challenge(context, this.getDefaultChallengeMessage(context), "password"));
    }

    private boolean existsKeycloakUser(KeycloakSession session, RealmModel realm, String username) {
        return getKeycloakUser(session, realm, username) != null;
    }

    private UserModel getKeycloakUser(KeycloakSession session, RealmModel realm, String username) {
        return KeycloakModelUtils.findUserByNameOrEmail(session, realm,
            Objects.requireNonNullElse(username, "").trim());
    }

    private UserModel createKeycloakUser(
        KeycloakSession session,
        MultivaluedMap<String, String> formData,
        String firstname,
        String lastname
    ) throws ValidationException {
        var password = new WeakReference<>(formData.getFirst(PASSWORD));
        formData = prepareFormParametersForUserCreation(formData, firstname, lastname);
        var profileProvider = session.getProvider(UserProfileProvider.class);
        var profile = profileProvider.create(REGISTRATION, formData);

        var user = profile.create();
        user.setEnabled(true);
        user.setEmail(formData.getFirst(EMAIL));
        user.setFirstName(firstname);
        user.setLastName(lastname);
        profile.update();

        var passwordProvider = (PasswordCredentialProvider) session.getProvider(CredentialProvider.class,
            PasswordCredentialProviderFactory.PROVIDER_ID);
        passwordProvider.createCredential(session.getContext().getRealm(), user, password.get());

        return user;
    }

    private MultivaluedMap<String, String> prepareFormParametersForUserCreation(
        MultivaluedMap<String, String> formParams,
        String firstname,
        String lastname
    ) {
        var copy = new MultivaluedHashMap<>(formParams);
        copy.put(EMAIL, copy.get(FORM_USERNAME));
        copy.put(FIRST_NAME, List.of(firstname));
        copy.put(LAST_NAME, List.of(lastname));
        copy.remove(PASSWORD);
        return copy;
    }
}
