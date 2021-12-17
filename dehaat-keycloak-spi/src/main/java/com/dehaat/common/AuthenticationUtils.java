package com.dehaat.common;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author sushil
 **/
public class AuthenticationUtils {
    /**
     * This class is meant to expose keycloak core code
     */
    private static final String MOBILE_NUMBER = "mobile_number";

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

    public static UserModel getUserFromMobile(String mobileNumber, KeycloakSession session) {
        UserModel user = null;
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            return null;
        }
        Stream<UserModel> userStream = session.users().searchForUserByUserAttributeStream(session.getContext().getRealm(), MOBILE_NUMBER, mobileNumber);
        List<UserModel> usersList = userStream.collect(Collectors.toList());

        /** user exists **/
        if (usersList.size() > 0) {
            user = usersList.get(0);
        }
        return user;
    }

    public static boolean isDisabledByBruteForce(AuthenticationFlowContext context, UserModel user) {
        String bruteForceError = AuthenticatorUtils.getDisabledByBruteForceEventError(context.getProtector(), context.getSession(), context.getRealm(), user);
        if (bruteForceError != null) {
            return true;
        }
        return false;
    }
}
