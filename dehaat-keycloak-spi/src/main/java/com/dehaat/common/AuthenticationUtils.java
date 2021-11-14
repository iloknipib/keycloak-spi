package com.dehaat.common;

import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author sushil
 **/
public class AuthenticationUtils {
    /**
     * This class is meant to expose keycloak core code
     */
    public static void generateSecret(String userID, KeycloakSession session) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userID);
        RealmModel realm = session.getContext().getRealm();
        CredentialProvider otpCredentialProvider = session.getProvider(CredentialProvider.class, "keycloak-otp");
        OTPCredentialModel credentialModel = OTPCredentialModel.createFromPolicy(realm, HmacOTP.generateSecret(20));
        otpCredentialProvider.createCredential(realm, user, credentialModel);
    }

    public static UserModel createUser(MultivaluedMap<String, String> inputData, KeycloakSession session) throws ValidationException {
        String username = KeycloakModelUtils.generateId();
        inputData.putSingle(UserModel.USERNAME, username);
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);
        UserProfile profile = profileProvider.create(UserProfileContext.REGISTRATION_USER_CREATION, inputData);
        UserModel user = profile.create();
        return user;
    }

    public static OTPCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (OTPCredentialProvider) session.getProvider(CredentialProvider.class, "keycloak-otp");
    }
}
