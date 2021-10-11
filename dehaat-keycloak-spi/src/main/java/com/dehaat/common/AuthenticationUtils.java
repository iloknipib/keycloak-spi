package com.dehaat.common;

import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuthenticationUtils {
    /**
     * This class is meant to expose keycloak core code
     *
     */
    public static void generateSecret(String userID, KeycloakSession session) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userID);
        RealmModel realm = session.getContext().getRealm();
        CredentialProvider otpCredentialProvider = session.getProvider(CredentialProvider.class, "keycloak-otp");
        OTPCredentialModel credentialModel = OTPCredentialModel.createFromPolicy(realm, HmacOTP.generateSecret(20));
        PasswordCredentialProvider provider = (PasswordCredentialProvider) session.getProvider(CredentialProvider.class, "keycloak-password");
        provider.createCredential(realm, user, "");
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

    public static List<UserModel> getUserEntityByUserId(EntityManager em, String userId, RealmModel realm, KeycloakSession session) throws ValidationException {
        UserEntity entity = em.find(UserEntity.class, userId);
        Stream<UserModel> userStream = session.users().searchForUserStream(realm, entity.getUsername());
        List<UserModel> usersList = userStream.collect(Collectors.toList());
        return usersList;
    }

}
