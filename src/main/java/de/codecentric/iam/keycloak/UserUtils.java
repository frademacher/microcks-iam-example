package de.codecentric.iam.keycloak;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;

import static org.keycloak.userprofile.UserProfileContext.UPDATE_PROFILE;

/**
 * Common utility functions for Keycloak user handling.
 */
public abstract class UserUtils {
    private UserUtils() {
        // NOOP
    }

    public static void updateKeycloakUser(KeycloakSession session, UserModel user) throws ValidationException {
        var profileProvider = session.getProvider(UserProfileProvider.class);
        var profile = profileProvider.create(UPDATE_PROFILE, user.getAttributes(), user);
        profile.update();
    }
}
