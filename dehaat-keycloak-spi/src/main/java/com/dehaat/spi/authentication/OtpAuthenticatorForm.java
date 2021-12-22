package com.dehaat.spi.authentication;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.common.Helper;
import com.dehaat.common.HoneybadgerErrorReporter;
import com.dehaat.common.MobileNumberValidator;
import com.dehaat.service.*;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static com.dehaat.common.AuthenticationUtils.getUserFromMobile;

/**
 * @author sushil
 */
public class OtpAuthenticatorForm extends OTPFormAuthenticator {

    private static final String TPL_CODE = "login-sms.ftl";
    private static final String MOBILE_NUMBER = "mobile_number";
    private static final String SENT_SMS_MOBILE_FAIL = "sms service failed to send otp";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getRealm().getAuthenticatorConfigByAlias("mobile_otp_config");
        KeycloakSession session = context.getSession();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String mobileNumber = formData.getFirst("username");
        boolean isValidMobile = false;
        if (mobileNumber != null) {
            isValidMobile = MobileNumberValidator.isValid(mobileNumber);
        }

        if (isValidMobile) {
            UserModel user = getUserFromMobile(mobileNumber, context.getSession());

            if (user != null && user.isEnabled() && !AuthenticationUtils.isDisabledByBruteForce(context,user)) {
                int ttl = Integer.parseInt(config.getConfig().get("ttl"));
                context.form().setAttribute(MOBILE_NUMBER, mobileNumber);
                try {
                    OTPGenerator otpGenerator = new OTPGeneratorService(session, user);
                    String otp = otpGenerator.createOTP();
                    String clientId = context.getSession().getContext().getClient().getId();
                    SMSSender sender = new SendOTPService(mobileNumber, otp, ttl, clientId);
                    boolean isMailSent = sender.send();
                    if (isMailSent)
                        context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
                    else
                        throw new Exception(SENT_SMS_MOBILE_FAIL);
                } catch (Exception e) {
                    HoneybadgerErrorReporter.getReporter().reportError(new Exception(e.getMessage()));
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
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        String mobileNumber = inputData.getFirst(MOBILE_NUMBER);
        UserModel user = getUserFromMobile(mobileNumber, context.getSession());

        boolean valid;

        if (user != null && user.isEnabled() && !AuthenticationUtils.isDisabledByBruteForce(context,user)) {

            /** get otp from form **/
            String otp = inputData.getFirst("otp");
            if (otp != null && !otp.isEmpty()) {
                otp = otp.trim();
            }

            if (otp == null) {
                Response challengeResponse = this.challenge(context, "invalidTotpMessage", "totp");
                context.challenge(challengeResponse);
            } else {
                boolean isProdEnv = Helper.isProdEnv();
                if (!isProdEnv) {
                    if (otp.equals("123456")) {
                        context.success();
                        context.setUser(user);
                        return;
                    }
                }

                context.getAuthenticationSession().setAuthNote("ATTEMPTED_USERNAME", user.getUsername());
                OTPValidator otpValidator = new OTPValidatorService(context, user);
                try {
                    valid = otpValidator.isValid(otp);
                    if (valid) {
                        context.success();
                        context.setUser(user);
                    } else {
                        context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
                        Response challengeResponse = this.challenge(context, "invalidTotpMessage", "totp", mobileNumber);
                        context.form().setAttribute(MOBILE_NUMBER, mobileNumber);
                        context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
                    }
                } catch (Exception e) {
                    context.getEvent().user(user).error("invalid_user_credentials");
                    Response challengeResponse = this.challenge(context, "invalidTotpMessage", "totp", mobileNumber);
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
                }
            }
        } else {
            /** mobile not registered **/
            context.form().setAttribute(MOBILE_NUMBER, mobileNumber);
            Response challengeResponse = this.challenge(context, "invalidTotpMessage", AuthenticationManager.FORM_USERNAME, mobileNumber);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        }
    }

    protected Response challenge(AuthenticationFlowContext context, String error, String field, String mobileNumber) {
        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        form.setAttribute(MOBILE_NUMBER, mobileNumber);

        if (error != null) {
            if (field != null) {
                form.addError(new FormMessage(field, error));
            } else {
                form.setError(error, new Object[0]);
            }
        }

        return form.createForm(TPL_CODE);
    }

    @Override
    public boolean requiresUser() {
        return false;
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
