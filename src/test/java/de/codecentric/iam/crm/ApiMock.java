package de.codecentric.iam.crm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.microcks.testcontainers.MicrocksContainer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.plexus.util.StringUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.MacSignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.keycloak.OAuth2Constants.SCOPE_EMAIL;
import static org.keycloak.OAuth2Constants.SCOPE_OPENID;
import static org.keycloak.OAuth2Constants.SCOPE_PROFILE;
import static org.keycloak.crypto.Algorithm.HS256;
import static org.keycloak.util.TokenUtil.TOKEN_TYPE_BEARER;

/**
 * Helper class for API mock configuration within a given {@link MicrocksContainer}.
 */
class ApiMock {
    private static final String API_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJzdWIiOiJDUk0gQVBJIiwiaWF0IjoxNTE2MjM5MDIyfQ.3myIhJ8VBtyFcokU-CgA42OoEJZba4SnDAxQzengRlY";
    private static final String EXISTING_CUSTOMER_EMAIL_TEMPLATE_PARAMETER = "${EXISTING_CUSTOMER_EMAIL}";
    private static final String EXISTING_CUSTOMER_LOGIN_TOKEN_TEMPLATE_PARAMETER = "${EXISTING_CUSTOMER_LOGIN_TOKEN}";
    private static final String EXISTING_CUSTOMER_PASSWORD_TEMPLATE_PARAMETER = "${EXISTING_CUSTOMER_PASSWORD}";
    private static final String NEW_CUSTOMER_EMAIL_TEMPLATE_PARAMETER = "${NEW_CUSTOMER_EMAIL}";
    private static final String NEW_CUSTOMER_LOGIN_TOKEN_TEMPLATE_PARAMETER = "${NEW_CUSTOMER_LOGIN_TOKEN}";
    private static final String NEW_CUSTOMER_PASSWORD_TEMPLATE_PARAMETER = "${NEW_CUSTOMER_PASSWORD}";

    private final CustomerMockData existingCustomerMockData;
    private final String keycloakApiConfigResourcePath;
    private final MicrocksContainer microcksContainer;
    private final CustomerMockData newCustomerMockData;
    private final String specifiedApiTitle;
    private final String specifiedApiVersion;

    /**
     * Constructor.
     */
    ApiMock(String apiSpecResourcePath, MicrocksContainer microcksContainer, String apiMetadataResourcePath,
        String apiExamplesResourcePath, String keycloakApiConfigResourcePath) {
        this.microcksContainer = microcksContainer;
        this.keycloakApiConfigResourcePath = keycloakApiConfigResourcePath;

        // Microcks builds the URLs of mocks for OpenAPI definitions from definitions' titles and versions. Therefore,
        // parse title and version from a given OpenAPI definition here.
        var specifiedApiTitleAndVersion = parseSpecifiedApiTitleAndVersion(apiSpecResourcePath);
        specifiedApiTitle = specifiedApiTitleAndVersion.getLeft();
        specifiedApiVersion = specifiedApiTitleAndVersion.getRight();

        // Setup objects with mock data for an existing CRM customer (test of migrating login Keycloak authenticator)
        // and a new CRM customer (test of CRM registration Keycloak action)
        var mockedApiUrl = getMockedApiUrl(microcksContainer, specifiedApiTitle, specifiedApiVersion);
        existingCustomerMockData = new CustomerMockData(
                "john.doe@example.com",
                "securePassword1!",
                "John",
                "Doe",
                "1234567890",
                mockedApiUrl
            );
        newCustomerMockData = new CustomerMockData(
                "jane.roe@example.com",
                "completelyDifferentSecurePassword2!",
                "Jane",
                "Roe",
                "0987654321",
                mockedApiUrl
            );

        // Import the given Microcks API Examples file (non-intrusive extension of Microcks mocks with mock requests and
        // responses) and Microcks API Metadata file (retrofitted mock behavior)
        importExamplesWithRuntimeData(apiExamplesResourcePath, existingCustomerMockData, newCustomerMockData);
        importMetadataWithRuntimeData(apiMetadataResourcePath, microcksContainer, existingCustomerMockData,
            newCustomerMockData);
    }

    /**
     * Helper to parse title and version from YAML-based OpenAPI definitions.
     */
    private Pair<String, String> parseSpecifiedApiTitleAndVersion(String apiSpecResourcePath) {
        try {
            var mapper = new ObjectMapper(new YAMLFactory());
            var spec = mapper.readTree(getClass().getClassLoader().getResourceAsStream(apiSpecResourcePath));
            return Pair.of(
                spec.get("info").get("title").asText(),
                spec.get("info").get("version").asText()
            );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Class for CRM customer mock data.
     */
    static class CustomerMockData {
        private final String email;
        private final String password;
        private final String firstname;
        private final String lastname;
        private final String customerId;
        private final String loginToken;

        public CustomerMockData(String email, String password, String firstname, String lastname, String customerId,
            String loginTokenIssuer) {
            this.email = email;
            this.password = password;
            this.firstname = firstname;
            this.lastname = lastname;
            this.customerId = customerId;
            loginToken = createAccessToken(loginTokenIssuer, customerId);
        }

        /**
         * Create a JWT token with a 5-minutes expiration that shall act as an access token from CRM customer mock data.
         */
        private String createAccessToken(String issuer, String subject) {
            var token = new AccessToken();
            long nowInSeconds = Time.currentTime();
            long fiveMinutesFromNowInSeconds = nowInSeconds + 300;
            token
                .type(TOKEN_TYPE_BEARER)
                .issuer(issuer)
                .subject(subject)
                .iat(nowInSeconds)
                .exp(fiveMinutesFromNowInSeconds);
            token.setScope(String.join(" ", SCOPE_OPENID, SCOPE_PROFILE, SCOPE_EMAIL));

            var algorithm = HS256;
            var keyWrapper = new KeyWrapper();
            keyWrapper.setAlgorithm(algorithm);
            keyWrapper.setUse(KeyUse.SIG);
            keyWrapper.setSecretKey(
                new SecretKeySpec("secret".getBytes(UTF_8), JavaAlgorithm.getJavaAlgorithm(algorithm))
            );

            return new JWSBuilder().jsonContent(token).sign(new MacSignatureSignerContext(keyWrapper));
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public String getCustomerId() {
            return customerId;
        }

        public String getLoginToken() {
            return loginToken;
        }
    }

    /**
     * Import Microcks API Examples into a running Microcks Testcontainer.
     */
    private void importExamplesWithRuntimeData(
        String apiExamplesResourcePath,
        CustomerMockData existingCustomerMockData,
        CustomerMockData newCustomerMockData
    ) {
        // The import happens as a Microcks secondary artifact. The given Microcks API Examples file is also considered
        // a template with placeholders in the form ${...} and those placeholders are replaced by actual values in the
        // following step. This step, for example, replaces placeholders for the mock data of an existing CRM customer
        // with the actual mock data, e.g., the customer's login token derived at test runtime. Therefore, we are
        // maximum flexible in adding new mock customer data and use it in Microcks secondary artifacts as we go.
        importSecondaryArtifactRenderedFromTemplate(
            microcksContainer,
            apiExamplesResourcePath,
            Pair.of(EXISTING_CUSTOMER_EMAIL_TEMPLATE_PARAMETER, existingCustomerMockData.getEmail()),
            Pair.of(EXISTING_CUSTOMER_LOGIN_TOKEN_TEMPLATE_PARAMETER, existingCustomerMockData.getLoginToken()),
            Pair.of(EXISTING_CUSTOMER_PASSWORD_TEMPLATE_PARAMETER, existingCustomerMockData.getPassword()),
            Pair.of("${EXISTING_CUSTOMER_FIRSTNAME}", existingCustomerMockData.getFirstname()),
            Pair.of("${EXISTING_CUSTOMER_LASTNAME}", existingCustomerMockData.getLastname()),
            Pair.of(NEW_CUSTOMER_EMAIL_TEMPLATE_PARAMETER, newCustomerMockData.getEmail()),
            Pair.of(NEW_CUSTOMER_LOGIN_TOKEN_TEMPLATE_PARAMETER, newCustomerMockData.getLoginToken()),
            Pair.of(NEW_CUSTOMER_PASSWORD_TEMPLATE_PARAMETER, newCustomerMockData.getPassword())
        );
    }

    /**
     * Import a given resource as a Microcks secondary artifact (e.g., Microcks API Examples or Metadata). The resource
     * is considered a template with placeholders in the form ${...} and those placeholders are replaced by actual
     * values from the given template replacements.
     */
    @SafeVarargs
    private void importSecondaryArtifactRenderedFromTemplate(
        MicrocksContainer microcksContainer,
        String templatedResourcePath,
        Pair<String, String>... templateReplacements
    ) {
        try {
            var template = FileUtils.readFileToString(getResourceAsFile(templatedResourcePath), UTF_8);
            var renderedTemplate = Arrays.stream(templateReplacements).reduce(
                    template,
                    (renderedSoFar, repl) -> StringUtils.replace(renderedSoFar, repl.getKey(), repl.getValue()),
                    (rendered1, rendered2) -> rendered1 + rendered2
                );
            var renderedTemplateFile = File.createTempFile("rendered-microcks-secondary-artifact", null);
            FileUtils.writeStringToFile(renderedTemplateFile, renderedTemplate, UTF_8);
            microcksContainer.importAsSecondaryArtifact(renderedTemplateFile);
            renderedTemplateFile.deleteOnExit();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private File getResourceAsFile(String relativeResourcePath) {
        var resourceUrl = Objects.requireNonNull(getClass().getClassLoader().getResource(relativeResourcePath))
            .getFile();
        return new File(URLDecoder.decode(resourceUrl, UTF_8));
    }

    /**
     * Retrieve a version of the JSON structure in the Keycloak secret with which access to the CRM API can be
     * configured with an actual API token and the URL of the CRM API set to that of the corresponding Microcks mock.
     * That is, if this secret is installed within a running Keycloak instance, Keycloak will interact with the mock for
     * the CRM API when invoking the methods of {@link CrmApiFacade}.
     */
    String mockedKeycloakConfig() {
        try {
            var apiConfig = CrmApiConfig.parseConfigEntry(
                    FileUtils.readFileToString(getResourceAsFile(keycloakApiConfigResourcePath), UTF_8.name())
                );
            apiConfig.setUrl(getMockedApiUrl(microcksContainer, specifiedApiTitle, specifiedApiVersion));
            apiConfig.setApiToken(API_TOKEN);
            return JsonSerialization.writeValueAsPrettyString(apiConfig);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getMockedApiUrl(MicrocksContainer microcksContainer, String apiTitle, String apiVersion) {
        // To allow Keycloak for interacting with the mocked API, we need to provide the latter's URL within the Docker
        // network to which both the Keycloak and Microcks Testcontainer belong. Outside the network, the Keycloak
        // container won't be able to reach the mock.
        var mockBaseUrlInContainerNetwork = String.format(
                "http://%s:%s",
                microcksContainer.getNetworkAliases().getFirst(),
                MicrocksContainer.MICROCKS_HTTP_PORT
            );

        return mockBaseUrlInContainerNetwork + "/rest/" + apiTitle + "/" + apiVersion;
    }

    /**
     * Import Microcks API Metadata into a running Microcks Testcontainer.
     */
    private void importMetadataWithRuntimeData(String apiMetadataResourcePath, MicrocksContainer microcksContainer,
        CustomerMockData existingCustomerMockData, CustomerMockData newCustomerMockData) {
        importSecondaryArtifactRenderedFromTemplate(
            microcksContainer,
            apiMetadataResourcePath,
            Pair.of("${API_TOKEN}", API_TOKEN),
            Pair.of(EXISTING_CUSTOMER_EMAIL_TEMPLATE_PARAMETER, existingCustomerMockData.getEmail()),
            Pair.of(EXISTING_CUSTOMER_LOGIN_TOKEN_TEMPLATE_PARAMETER, existingCustomerMockData.getLoginToken()),
            Pair.of(EXISTING_CUSTOMER_PASSWORD_TEMPLATE_PARAMETER, existingCustomerMockData.getPassword()),
            Pair.of(NEW_CUSTOMER_EMAIL_TEMPLATE_PARAMETER, newCustomerMockData.getEmail()),
            Pair.of(NEW_CUSTOMER_LOGIN_TOKEN_TEMPLATE_PARAMETER, newCustomerMockData.getLoginToken()),
            Pair.of(NEW_CUSTOMER_PASSWORD_TEMPLATE_PARAMETER, newCustomerMockData.getPassword()),
            Pair.of("${NEW_CUSTOMER_ID}", newCustomerMockData.getCustomerId())
        );
    }

    public String getSpecifiedApiTitle() {
        return specifiedApiTitle;
    }

    public String getSpecifiedApiVersion() {
        return specifiedApiVersion;
    }

    CustomerMockData getExistingCustomerMockData() {
        return existingCustomerMockData;
    }

    public CustomerMockData getNewCustomerMockData() {
        return newCustomerMockData;
    }
}
