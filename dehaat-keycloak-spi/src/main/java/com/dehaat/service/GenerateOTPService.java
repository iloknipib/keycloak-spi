package com.dehaat.service;

import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;

public class GenerateOTPService implements OTPGenerator {

    private KeycloakSession session;
    private UserModel user;
    private String algo;
    private int numberDigits;
    private int timeIntervalInSeconds;
    private int lookAheadWindow;

    public GenerateOTPService(KeycloakSession session, UserModel user, String algo, int numberDigits, int timeIntervalInSeconds, int lookAheadWindow) {
        this.session = session;
        this.user = user;
        this.algo = algo;
        this.numberDigits = numberDigits;
        this.timeIntervalInSeconds = timeIntervalInSeconds;
        this.lookAheadWindow = lookAheadWindow;
    }

    @Override
    public String createOTP() {
        TimeBasedOTP timeBasedOTP = new TimeBasedOTP(algo, numberDigits, timeIntervalInSeconds, lookAheadWindow);
        OTPCredentialModel defaultOtpCredential = getCredentialProvider(session)
                .getDefaultCredential(session, session.getContext().getRealm(), user);
        String otp = timeBasedOTP.generateTOTP(defaultOtpCredential.getSecretData());
        return otp;
    }
    private OTPCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (OTPCredentialProvider) session.getProvider(CredentialProvider.class, "keycloak-otp");
    }

}
