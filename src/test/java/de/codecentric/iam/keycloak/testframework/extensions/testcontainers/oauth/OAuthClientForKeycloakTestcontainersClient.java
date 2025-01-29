package de.codecentric.iam.keycloak.testframework.extensions.testcontainers.oauth;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.TokenIntrospectionRequest;
import com.nimbusds.oauth2.sdk.TokenIntrospectionResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.TokenRevocationRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

public class OAuthClientForKeycloakTestcontainersClient {
    private final ManagedRealm realm;
    private final ManagedClient client;
    private final OAuthCallbackServer callbackServer;
    private OIDCProviderMetadata oidcProviderMetadata;

    public OAuthClientForKeycloakTestcontainersClient(ManagedRealm realm, String existingClientId) {
        this.realm = realm;
        this.client = getExistingClient(existingClientId);
        this.callbackServer = new OAuthCallbackServer();
    }

    private ManagedClient getExistingClient(String existingClientId) {
        var clientRepresentation = realm.admin().clients().findByClientId(existingClientId).getFirst();
        return new ManagedClient(clientRepresentation, realm.admin().clients().get(clientRepresentation.getId()));
    }

    public TokenResponse clientCredentialGrant() throws IOException, GeneralException {
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();
        ClientAuthentication clientAuthentication = getClientAuthentication();
        URI tokenEndpoint = getOIDCProviderMetadata().getTokenEndpointURI();

        TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, clientGrant);
        return TokenResponse.parse(tokenRequest.toHTTPRequest().send());
    }

    public TokenResponse resourceOwnerCredentialGrant(String username, String password) {
        try {
            ResourceOwnerPasswordCredentialsGrant credentialsGrant
                = new ResourceOwnerPasswordCredentialsGrant(username, new Secret(password));
            ClientAuthentication clientAuthentication = getClientAuthentication();
            URI tokenEndpoint = getOIDCProviderMetadata().getTokenEndpointURI();

            TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, credentialsGrant);
            return TokenResponse.parse(tokenRequest.toHTTPRequest().send());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TokenResponse tokenRequest(AuthorizationCode authorizationCode) throws IOException, GeneralException {
        AuthorizationGrant grant = new AuthorizationCodeGrant(authorizationCode, callbackServer.getRedirectionUri());
        ClientAuthentication clientAuthentication = getClientAuthentication();
        URI tokenEndpoint = getOIDCProviderMetadata().getTokenEndpointURI();

        TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, grant);
        return TokenResponse.parse(tokenRequest.toHTTPRequest().send());
    }

    public TokenIntrospectionResponse introspection(AccessToken accessToken) throws IOException, GeneralException {
        ClientAuthentication clientAuthentication = getClientAuthentication();
        URI introspectionEndpoint = getOIDCProviderMetadata().getIntrospectionEndpointURI();

        TokenIntrospectionRequest introspectionRequest
            = new TokenIntrospectionRequest(introspectionEndpoint, clientAuthentication, accessToken);
        return TokenIntrospectionResponse.parse(introspectionRequest.toHTTPRequest().send());
    }

    public HTTPResponse revokeAccessToken(AccessToken token) throws GeneralException, IOException {
        URI revocationEndpoint = getOIDCProviderMetadata().getRevocationEndpointURI();
        TokenRevocationRequest revocationRequest = new TokenRevocationRequest(
                revocationEndpoint,
                getClientAuthentication(),
                token
            );
        return revocationRequest.toHTTPRequest().send();
    }

    public URL authorizationRequest() {
        try {
            URI authorizationEndpoint = getOIDCProviderMetadata().getAuthorizationEndpointURI();
            State state = new State();
            ClientID clientID = new ClientID(client.getClientId());

            AuthorizationRequest authorizationRequest
                = new AuthorizationRequest.Builder(new ResponseType(ResponseType.Value.CODE), clientID)
                    .state(state)
                    .redirectionURI(callbackServer.getRedirectionUri())
                    .endpointURI(authorizationEndpoint)
                    .build();

            return authorizationRequest.toURI().toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<URI> getCallbacks() {
        return callbackServer.getCallbacks();
    }

    public void close() {
        callbackServer.close();
    }

    private ClientAuthentication getClientAuthentication() {
        ClientID clientID = new ClientID(client.getClientId());
        Secret clientSecret = new Secret(client.getSecret());
        return new ClientSecretBasic(clientID, clientSecret);
    }

    private OIDCProviderMetadata getOIDCProviderMetadata() throws GeneralException, IOException {
        if (oidcProviderMetadata == null) {
            Issuer issuer = new Issuer(realm.getBaseUrl());
            oidcProviderMetadata = OIDCProviderMetadata.resolve(issuer);
        }
        return oidcProviderMetadata;
    }

}
