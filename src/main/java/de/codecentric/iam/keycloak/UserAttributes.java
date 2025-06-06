package de.codecentric.iam.keycloak;

/**
 * Common user attribute constants.
 */
public abstract class UserAttributes {
    public static final String CRM_CUSTOMER_ADDRESS_ATTRIBUTE = "crmCustomerAddress";
    public static final String CRM_CUSTOMER_ID_ATTRIBUTE = "crmCustomerId";

    private UserAttributes() {
        // NOOP
    }
}
