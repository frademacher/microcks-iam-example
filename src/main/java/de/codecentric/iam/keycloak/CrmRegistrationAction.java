package de.codecentric.iam.keycloak;

import com.google.auto.service.AutoService;
import de.codecentric.iam.crm.CrmApiFacade;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.forms.RegistrationUserCreation;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;

import java.util.List;

import static de.codecentric.iam.keycloak.UserAttributes.CRM_CUSTOMER_ADDRESS_ATTRIBUTE;
import static de.codecentric.iam.keycloak.UserAttributes.CRM_CUSTOMER_ID_ATTRIBUTE;
import static de.codecentric.iam.keycloak.UserUtils.updateKeycloakUser;
import static org.apache.http.HttpStatus.SC_OK;
import static org.keycloak.models.UserModel.EMAIL;
import static org.keycloak.models.UserModel.FIRST_NAME;
import static org.keycloak.models.UserModel.LAST_NAME;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

/**
 * Implementation of Keycloak's {@link RegistrationUserCreation} form action which creates a new CRM customer from a
 * user that currently registers for Keycloak.
 */
@AutoService(FormActionFactory.class)
public class CrmRegistrationAction extends RegistrationUserCreation {
    @Override
    public String getId() {
        return "crm-" + super.getId();
    }

    @Override
    public String getDisplayType() {
        return "CRM " + super.getDisplayType();
    }

    /**
     * Validate form inputs.
     */
    @Override
    public void validate(ValidationContext context) {
        var formData = context.getHttpRequest().getDecodedFormParameters();
        var email = formData.getFirst(EMAIL);
        if (StringUtils.isBlank(email))
            return;

        // Make sure that CRM customer doesn't exist already
        var customerExists = CrmApiFacade
            .session(context.getSession())
            .existsCustomer(email);
        if (customerExists.orElse(true)) {
            context.getEvent().detail(Details.EMAIL, email);
            context.error(Errors.EMAIL_IN_USE);
            // Prevent NPE from FormAuthenticationFlow.processAction()
            context.validationError(formData, List.of());
        } else
            super.validate(context);
    }

    /**
     * Handle validation success and proceed with actual form handling.
     */
    @Override
    public void success(FormContext context) {
        super.success(context);

        /* Create the CRM customer */
        var formData = context.getHttpRequest().getDecodedFormParameters();
        var email = formData.getFirst(EMAIL);
        var password = formData.getFirst(PASSWORD);
        var firstname = formData.getFirst(FIRST_NAME);
        var lastname = formData.getFirst(LAST_NAME);
        var crmCustomerCreated = CrmApiFacade
            .session(context.getSession())
            .createCustomer(email, password, firstname, lastname);
        if (!crmCustomerCreated.orElse(false))
            return;

        /*
         * Acquire CRM customer login token, and use it to extract CRM customer ID and address, which are then stored as
         * attributes for the Keycloak user
         */
        var crmLoginResponse = CrmApiFacade
            .session(context.getSession())
            .login(email, password);
        if (crmLoginResponse.isEmpty() || crmLoginResponse.get().httpStatus() != SC_OK)
            return;

        var crmLoginTokenJwt = crmLoginResponse.get().loginToken();
        AccessToken crmLoginToken;
        try {
            crmLoginToken = new JWSInput(crmLoginTokenJwt).readJsonContent(AccessToken.class);
        } catch (Exception ex) {
            return;
        }

        var crmCustomer = CrmApiFacade
            .session(context.getSession())
            .getCustomer(crmLoginTokenJwt);
        if (crmCustomer.isEmpty() || crmCustomer.get().httpStatus() != SC_OK)
            return;

        context.getUser().setFirstName(crmCustomer.get().firstname());
        context.getUser().setLastName(crmCustomer.get().lastname());
        context.getUser().setAttribute(CRM_CUSTOMER_ID_ATTRIBUTE, List.of(crmLoginToken.getSubject()));
        context.getUser().setAttribute(CRM_CUSTOMER_ADDRESS_ATTRIBUTE, List.of(crmCustomer.get().address()));
        updateKeycloakUser(context.getSession(), context.getUser());
    }
}
