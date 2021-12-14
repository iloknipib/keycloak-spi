package com.dehaat.service;

import com.dehaat.common.AuthenticationUtils;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;

import java.nio.charset.StandardCharsets;

/**
 * @author sushil
 */
public class OTPValidatorService implements OTPValidator{
    private int ttl;
    private int length;
    private OTPCredentialModel OtpCredential;
    private UserModel user;


    public OTPValidatorService(AuthenticationFlowContext context, UserModel user) {
        this.user=user;
        init(context);
    }

    @Override
    public boolean isValid(String otp) throws Exception{
        TimeBasedOTP timeBasedOTP = new TimeBasedOTP("HmacSHA1", length, ttl, 1);
        try {
            String secretData = OtpCredential.getSecretData();
            boolean valid = timeBasedOTP.validateTOTP(otp, secretData.getBytes(StandardCharsets.UTF_8));
            return valid;
        }catch (Exception ex){
            throw new Exception("Otp credentials not set for user");
        }
    }

    private void init(AuthenticationFlowContext context){
        AuthenticatorConfigModel config = context.getRealm().getAuthenticatorConfigByAlias("mobile_otp_config");
        OtpCredential = AuthenticationUtils.getCredentialProvider(context.getSession()).getDefaultCredential(context.getSession(), context.getRealm(), user);
        ttl = Integer.parseInt(config.getConfig().get("ttl"));
        length = Integer.parseInt(config.getConfig().get("length"));
    }
}
