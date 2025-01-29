package de.codecentric.iam.crm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

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

        public Optional<LoginResponse> login(String email, String password) {
            if (apiConfig == null)
                return Optional.empty();

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

            try (
                var response = SimpleHttp
                    .doPost(apiConfig.getUrl() + "/login", session)
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

        public Optional<GetCustomerResponse> getCustomer(String loginToken) {
            if (apiConfig == null)
                return Optional.empty();

            try (
                var response = SimpleHttp
                    .doGet(apiConfig.getUrl() + "/customers", session)
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

        public Optional<Boolean> createCustomer(String email, String password, String firstname,
            String lastname) {
            if (apiConfig == null)
                return Optional.empty();

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

            try (
                var response = SimpleHttp
                    .doPost(apiConfig.getUrl() + "/customers", session)
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

        public Optional<Boolean> existsCustomer(String email) {
            if (apiConfig == null)
                return Optional.empty();

            try (
                var response = SimpleHttp
                    .doGet(apiConfig.getUrl() + "/customers/" + email, session)
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
