package com.dehaat.spi.authentication;

import com.dehaat.common.MobileNumberValidator;
import com.dehaat.service.GenerateOTPService;
import com.dehaat.service.OTPGenerator;
import com.dehaat.service.SMSSender;
import com.dehaat.service.SendOTPService;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;

/**
 * @author sushil
 */
public class OtpAuthenticatorForm extends OTPFormAuthenticator {

    private static final String TPL_CODE = "login-sms.ftl";
    public static final String SELECTED_OTP_CREDENTIAL_ID = "selectedOtpCredentialId";
    private static final String MOBILE_NUMBER = "mobile_number";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        KeycloakSession session = context.getSession();
        UserModel user = context.getUser();
        String mobileNumber = user.getFirstAttribute(MOBILE_NUMBER);
        boolean isValidMobile = MobileNumberValidator.isValid(mobileNumber);

        if (isValidMobile) {
            if (user != null && !(user.getId()).isEmpty()) {

                int ttl = Integer.parseInt(config.getConfig().get("ttl"));
                int length = Integer.parseInt(config.getConfig().get("length"));
                String senderServiceURL = config.getConfig().get("SenderServiceURL");
                String token = config.getConfig().get("token");

                OTPCredentialModel defaultOtpCredential = getCredentialProvider(session)
                        .getDefaultCredential(context.getSession(), context.getRealm(), context.getUser());
                String credentialId = defaultOtpCredential == null ? "" : defaultOtpCredential.getId();
                context.getEvent().detail(Details.SELECTED_CREDENTIAL_ID, credentialId);
                context.form().setAttribute(SELECTED_OTP_CREDENTIAL_ID, credentialId);

                try {
                    OTPGenerator otpGenerator = new GenerateOTPService(session, user, "HmacSHA1", length, ttl, 1);
                    String otp = otpGenerator.createOTP();
                    System.out.println(otp);
                    SMSSender sender = new SendOTPService(senderServiceURL, mobileNumber, otp, ttl, token);
                    boolean isMailSent = sender.send();
                    if (isMailSent)
                        context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
                    else
                        throw new Exception();
                } catch (Exception e) {
                    context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                            context.form().setError("smsAuthSmsNotSent", e.getMessage())
                                    .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
                }
            }
        } else {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("smsAuthSmsNotSent", "Invalid Mobile Number")
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }


    @Override
    public void action(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        String otp = inputData.getFirst("otp");
        OTPCredentialModel defaultOtpCredential = this.getCredentialProvider(context.getSession()).getDefaultCredential(context.getSession(), context.getRealm(), context.getUser());

        int ttl = Integer.parseInt(config.getConfig().get("ttl"));
        int length = Integer.parseInt(config.getConfig().get("length"));

        TimeBasedOTP timeBasedOTP = new TimeBasedOTP("HmacSHA1", length, ttl, 1);
        String secretData = defaultOtpCredential.getSecretData();

        UserModel userModel = context.getUser();
        if (this.enabledUser(context, userModel)) {
            if (otp == null) {
                Response challengeResponse = this.challenge(context, (String) null);
                context.challenge(challengeResponse);
            } else {
                boolean valid = timeBasedOTP.validateTOTP(otp, secretData.getBytes(StandardCharsets.UTF_8));
                if (!valid) {
                    context.getEvent().user(userModel).error("invalid_user_credentials");
                    Response challengeResponse = this.challenge(context, "invalidTotpMessage", "totp");
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
                } else {
                    context.success();
                }
            }
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

}
