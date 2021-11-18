package com.dehaat.service;

import com.dehaat.common.AuthenticationUtils;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;

/**
 * @author sushil
 */
public class OTPGeneratorService implements OTPGenerator {

    private String algo;
    private int length;
    private int ttl;
    private int lookAheadWindow;
    private OTPCredentialModel OtpCredential;

    public OTPGeneratorService(KeycloakSession session, UserModel user) {
        init(session, user);
    }

    @Override
    public String createOTP() throws Exception{
        TimeBasedOTP timeBasedOTP = new TimeBasedOTP(algo, length, ttl, lookAheadWindow);
        try {
            String otp = timeBasedOTP.generateTOTP(OtpCredential.getSecretData());
            return otp;
        }catch (Exception ex){
            throw new Exception("Otp credentials not set for user");
        }
    }

    private void init(KeycloakSession session, UserModel user) {
        AuthenticatorConfigModel config = session.getContext().getRealm().getAuthenticatorConfigByAlias("mobile_otp_config");
        OtpCredential = AuthenticationUtils.getCredentialProvider(session).getDefaultCredential(session, session.getContext().getRealm(), user);
        ttl = Integer.parseInt(config.getConfig().get("ttl"));
        length = Integer.parseInt(config.getConfig().get("length"));
        lookAheadWindow = 1;
        algo = "HmacSHA1";
    }

}
