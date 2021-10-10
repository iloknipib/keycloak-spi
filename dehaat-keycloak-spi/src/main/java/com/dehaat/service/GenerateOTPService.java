package com.dehaat.service;

import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;

public class GenerateOTPService {

    private KeycloakSession session;

    public GenerateOTPService() {
    }

    public GenerateOTPService(KeycloakSession session) {
        this.session = session;
    }

    public boolean generateOTPAndSend(String algo, int numberDigits, int timeIntervalInSeconds, int lookAheadWindow, UserModel user) {
        TimeBasedOTP timeBasedOTP = new TimeBasedOTP(algo, numberDigits, timeIntervalInSeconds, lookAheadWindow);
        OTPCredentialModel defaultOtpCredential = getCredentialProvider(session)
                .getDefaultCredential(session, session.getContext().getRealm(), user);
        String otp = timeBasedOTP.generateTOTP(defaultOtpCredential.getSecretData());
        System.out.println("OTP generated " + otp);
        boolean isOTPsent = MailManService.createMailManRequest("https://mailman.api.dehaatagri.com/sms/bulk", user.getFirstAttribute("mobile_number"), otp, timeIntervalInSeconds);
        return isOTPsent;
    }

    public OTPCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (OTPCredentialProvider) session.getProvider(CredentialProvider.class, "keycloak-otp");
    }

}
