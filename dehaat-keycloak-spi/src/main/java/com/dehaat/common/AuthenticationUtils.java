package com.dehaat.common;

import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;

public class AuthenticationUtils {

    public static void generateSecret(String userID, KeycloakSession session) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userID);
        RealmModel realm = session.getContext().getRealm();
        CredentialProvider otpCredentialProvider = session.getProvider(CredentialProvider.class, "keycloak-otp");
        OTPCredentialModel credentialModel = OTPCredentialModel.createFromPolicy(realm, HmacOTP.generateSecret(20));
        PasswordCredentialProvider provider = (PasswordCredentialProvider) session.getProvider(CredentialProvider.class, "keycloak-password");
        provider.createCredential(realm, user, "");
        otpCredentialProvider.createCredential(realm, user, credentialModel);

    }

}
