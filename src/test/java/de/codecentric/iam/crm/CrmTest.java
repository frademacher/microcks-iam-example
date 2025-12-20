package de.codecentric.iam.crm;

import de.codecentric.iam.keycloak.testframework.extensions.testcontainers.TestcontainersKeycloakServerConfig;
import de.codecentric.iam.keycloak.testframework.extensions.testcontainers.TestcontainersKeycloakServerConfigBuilder;
import de.codecentric.iam.keycloak.testframework.extensions.testcontainers.TestcontainersKeycloakServerSupplier;
import de.codecentric.iam.keycloak.testframework.ui.page.RegistrationPage;
import io.github.microcks.testcontainers.MicrocksContainer;
import org.codehaus.plexus.util.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.codecentric.iam.crm.CrmApiConfig.API_CONFIG_SECRET_NAME;
import static de.codecentric.iam.keycloak.UserAttributes.CRM_CUSTOMER_ADDRESS_ATTRIBUTE;
import static de.codecentric.iam.keycloak.UserAttributes.CRM_CUSTOMER_ID_ATTRIBUTE;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Class holding Keycloak integration tests with the CRM system.
 */
@Testcontainers
@KeycloakIntegrationTest(config = CrmTest.KeycloakTestcontainerConfig.class)
class CrmTest {
    private static final String TEST_REALM_NAME = "test";
    private static final String API_SPEC_RESOURCE_PATH = "crm-api.yaml";
    private static final Network CONTAINER_NETWORK = Network.newNetwork();
    private static final String KEYCLOAK_API_CONFIG_RESOURCE_PATH = String.format(
            "keycloak/secrets/%s_%s",
            TEST_REALM_NAME,
            StringUtils.replace(API_CONFIG_SECRET_NAME, "_", "__")
        );

    private static ApiMock apiMock;

    @Container
    static final MicrocksContainer microcksContainer = new MicrocksContainer("quay.io/microcks/microcks-uber:1.13.1")
        .withNetwork(CONTAINER_NETWORK)
        .withMainArtifacts(API_SPEC_RESOURCE_PATH);

    /**
     * Prepare the API mock object which provides tests, among others, with mock data of CRM customers.
     */
    @BeforeAll
    static void prepareApiMock() {
        apiMock = new ApiMock(API_SPEC_RESOURCE_PATH, microcksContainer, "crm-api/metadata.yaml",
            "crm-api/examples.yaml", KEYCLOAK_API_CONFIG_RESOURCE_PATH);
    }

    /**
     * Configuration of Keycloak Testcontainer used by our extension of Keycloak's Test Framework to integrate with
     * Keycloak's Testcontainers Module.
     * @see TestcontainersKeycloakServerSupplier
     */
    static class KeycloakTestcontainerConfig extends TestcontainersKeycloakServerConfig {
        @Override
        public TestcontainersKeycloakServerConfigBuilder getBuilder() {
            return new TestcontainersKeycloakServerConfigBuilder()
                .withCopyClasspathResourceToContainer("keycloak/keycloak.conf", "/opt/keycloak/conf/keycloak.conf")
                .withWriteStringToContainerFile(apiMock.mockedKeycloakConfig(),
                    "/opt/" + KEYCLOAK_API_CONFIG_RESOURCE_PATH)
                .withNetwork(CONTAINER_NETWORK)
                .withDebugFixedPort(32781, false)
                .withProviderClassesFrom("target/classes")
                .withRealmImportFile("keycloak/test-realm.json");
        }
    }

    @InjectOAuthClient(config = OauthClientConfig.class)
    private OAuthClient oauthClient;

    static class OauthClientConfig implements ClientConfig {
        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder clientConfigBuilder) {
            return clientConfigBuilder.clientId("login-test");
        }
    }

    @InjectRealm(attachTo = TEST_REALM_NAME)
    private ManagedRealm testRealm;

    @InjectPage
    private RegistrationPage registrationPage;

    /**
     * Cleanup Keycloak login sessions on the test realm after each test.
     */
    @AfterEach
    void logoutUsers() {
        testRealm.admin().logoutAll();
    }

    /**
     * Test migrating login Keycloak authenticator.
     * @see de.codecentric.iam.keycloak.MigratingLoginFormAuthenticator
     */
    @Test
    void loginMigrationTest() {
        var userCountBeforeMigration = testRealm.admin().users().count();

        // Submit Keycloak login page with the mock data of the existing CRM customer
        var customerMockData = apiMock.getExistingCustomerMockData();
        oauthClient.doLogin(customerMockData.getEmail(), customerMockData.getPassword());

        // Assert that the mocked CRM API got actually called by the migrating login Keycloak authenticator
        assertThat(microcksContainer.verify(apiMock.getSpecifiedApiTitle(), apiMock.getSpecifiedApiVersion())).isTrue();

        // Assert that there is now one more Keycloak user as before
        assertThat(testRealm.admin().users().count()).isEqualTo(userCountBeforeMigration + 1);

        // Assert that the existing CRM customer got actually migrated into a Keycloak user
        var migratedUser = assertThat(testRealm.admin().users().search(customerMockData.getEmail()))
            .hasSize(1)
            .actual()
            .getFirst();
        assertThat(migratedUser.getFirstName()).isEqualTo(customerMockData.getFirstname());
        assertThat(migratedUser.getLastName()).isEqualTo(customerMockData.getLastname());
        assertThat(migratedUser.firstAttribute(CRM_CUSTOMER_ID_ATTRIBUTE)).isEqualTo(customerMockData.getCustomerId());
        assertThat(migratedUser.firstAttribute(CRM_CUSTOMER_ADDRESS_ATTRIBUTE)).matches(".+ \\d+, \\d+ .+, .+(, .+)?");
    }

    /**
     * Test CRM registration Keycloak action.
     * @see de.codecentric.iam.keycloak.CrmRegistrationAction
     */
    @Test
    void registrationTest() {
        // Navigate to Keycloak registration page, fill it with the mock data of the new CRM customer, and submit it
        var customerMockData = apiMock.getNewCustomerMockData();
        doRegistration(customerMockData.getEmail(), customerMockData.getPassword(), customerMockData.getPassword(),
            customerMockData.getFirstname(), customerMockData.getLastname());

        // Assert that the new CRM customer got actually registered as a Keycloak user
        var registeredUser = assertThat(testRealm.admin().users().search(customerMockData.getEmail()))
            .hasSize(1)
            .actual().getFirst();
        assertThat(registeredUser.getFirstName()).isEqualTo(customerMockData.getFirstname());
        assertThat(registeredUser.getLastName()).isEqualTo(customerMockData.getLastname());
        assertThat(registeredUser.firstAttribute(CRM_CUSTOMER_ID_ATTRIBUTE))
            .isEqualTo(customerMockData.getCustomerId());

        // Assert that registration is not possible for existing CRM customers (in this case, the user is required to
        // use the login form to migrate their existing CRM customer into a Keycloak user)
        try (var response = testRealm.admin().users().delete(registeredUser.getId())) {
            assertThat(response.getStatus()).isEqualTo(SC_NO_CONTENT);
            doRegistration(customerMockData.getEmail(), customerMockData.getPassword(), customerMockData.getPassword(),
                customerMockData.getFirstname(), customerMockData.getLastname());
            assertThat(testRealm.admin().users().search(customerMockData.getEmail())).isEmpty();
        }
    }

    /**
     * Perform Keycloak registration with the given data.
     */
    private void doRegistration(String email, String password, String passwordConfirmation, String firstname,
        String lastname) {
        oauthClient.openRegistrationForm();
        registrationPage.fillRegistration(email, password, passwordConfirmation, firstname, lastname);
        registrationPage.submit();
    }
}
