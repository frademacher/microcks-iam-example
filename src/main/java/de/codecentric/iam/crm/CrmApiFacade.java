package de.codecentric.iam.crm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * Facade for accessing operations of the CRM API at Keycloak runtime following in the form of a Fluent API.
 */
public class CrmApiFacade {
    private CrmApiFacade() {
        // NOOP
    }

    public static CrmApiFacadeWithSession session(KeycloakSession session) {
        return new CrmApiFacadeWithSession(Objects.requireNonNull(session, "Keycloak session mustn't be null"));
    }

    public static class CrmApiFacadeWithSession extends CrmApiFacade {
        private final CrmApiConfig.CrmApiConfigEntry apiConfig;
        private final KeycloakSession session;

        private static final Logger logger = Logger.getLogger(CrmApiFacadeWithSession.class);

        private CrmApiFacadeWithSession(KeycloakSession session) {
            apiConfig = CrmApiConfig.getConfigEntry(session);
            this.session = session;
        }

        /**
         * Facade method for the CRM API's POST Login operation. Requires the API's Bearer token from the Keycloak
         * configuration secret for the CRM API.
         */
        public Optional<LoginResponse> login(String email, String password) {
            if (apiConfig == null)
                return Optional.empty();

            // Prepare request from given parameter values
            JsonNode request;
            try {
                request = new ObjectMapper().readTree(String.format("""
                        {
                            "email": "%s",
                            "password": "%s"
                        }
                    """, email, password));
            } catch (Exception ex) {
                logger.error("Exception during preparation of login request", ex);
                return Optional.empty();
            }

            // Perform request and build response
            try (
                var response = SimpleHttp
                    .create(session)
                    .doPost(apiConfig.getUrl() + "/login")
                    .auth(apiConfig.getApiToken())
                    .acceptJson()
                    .json(request)
                    .asResponse()
            ) {
                return Optional.of(
                    new LoginResponse(response.getStatus(), response.asJson().path("login_token").asText())
                );
            } catch (IOException ex) {
                logger.error("Exception during login request", ex);
                return Optional.empty();
            }
        }

        public record LoginResponse(int httpStatus, String loginToken) {
            // NOOP
        }

        /**
         * Facade method for the CRM API's GET Customers operation, which returns a certain customer's details. The
         * customer is identified from the given token that stems from a successful CRM API POST Login request.
         */
        public Optional<GetCustomerResponse> getCustomer(String loginToken) {
            if (apiConfig == null)
                return Optional.empty();

            try (
                var response = SimpleHttp
                    .create(session)
                    .doGet(apiConfig.getUrl() + "/customers")
                    .auth(loginToken)
                    .asResponse()
            ) {
                return Optional.of(new GetCustomerResponse(
                    response.getStatus(),
                    response.asJson().path("firstname").asText(),
                    response.asJson().path("lastname").asText(),
                    response.asJson().path("address").asText()
                ));
            } catch (IOException ex) {
                logger.error("Exception during customer request", ex);
                return Optional.empty();
            }
        }

        public record GetCustomerResponse(int httpStatus, String firstname, String lastname, String address) {
            // NOOP
        }

        /**
         * Facade method for the CRM API's POST Customers operation, which creates a customer from the given details.
         *  Requires the API's Bearer token from the Keycloak configuration secret for the CRM API.
         */
        public Optional<Boolean> createCustomer(String email, String password, String firstname,
            String lastname) {
            if (apiConfig == null)
                return Optional.empty();

            // Prepare request from given parameter values
            JsonNode request;
            try {
                request = new ObjectMapper().readTree(String.format("""
                        {
                            "email": "%s",
                            "password": "%s",
                            "firstname": "%s",
                            "lastname": "%s"
                        }
                    """, email, password, firstname, lastname));
            } catch (Exception ex) {
                logger.error("Exception during preparation of create customer request", ex);
                return Optional.empty();
            }

            // Perform request and build response
            try (
                var response = SimpleHttp
                    .create(session)
                    .doPost(apiConfig.getUrl() + "/customers")
                    .auth(apiConfig.getApiToken())
                    .acceptJson()
                    .json(request)
                    .asResponse()
            ) {
                return Optional.of(response.getStatus() == SC_CREATED);
            } catch (IOException ex) {
                logger.error("Exception during customer request", ex);
                return Optional.empty();
            }
        }

        /**
         * Facade method for the CRM API's GET Customers/{email} operation, which allows for checking a customer's
         * existence in the CRM system form a given email. Requires the API's Bearer token from the Keycloak
         * configuration secret for the CRM API.
         */
        public Optional<Boolean> existsCustomer(String email) {
            if (apiConfig == null)
                return Optional.empty();

            try (
                var response = SimpleHttp
                    .create(session)
                    .doGet(apiConfig.getUrl() + "/customers/" + email)
                    .auth(apiConfig.getApiToken())
                    .asResponse()
            ) {
                return Optional.of(response.getStatus() == SC_OK);
            } catch (IOException ex) {
                logger.error("Exception during exists customer request", ex);
                return Optional.empty();
            }
        }
    }
}
