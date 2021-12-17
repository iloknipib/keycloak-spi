package com.dehaat.spi.authentication;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.common.Helper;
import com.dehaat.common.MobileNumberValidator;
import com.dehaat.service.OTPValidator;
import com.dehaat.service.OTPValidatorService;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static com.dehaat.common.AuthenticationUtils.getUserFromMobile;


/**
 * @author sushil
 **/
public class AndroidLoginForm implements Authenticator {

    private static final String TPL_CODE = "login-android.ftl";
    private static final String INVALID_MOBILE_ERROR = "Invalid Mobile Number";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        if (this.validateForm(context)) {
            context.success();
        }
    }

    protected boolean validateForm(AuthenticationFlowContext context) {
        UserModel user = getUserAndValidate(context);
        boolean isValidOtp = false;
        if (user != null && user.isEnabled() && !AuthenticationUtils.isDisabledByBruteForce(context, user)) {
            context.getAuthenticationSession().setAuthNote("ATTEMPTED_USERNAME", user.getUsername());
            isValidOtp = this.validateOTP(context, user);
            if (isValidOtp) {
                context.setUser(user);
            }
        }
        return isValidOtp;
    }

    protected UserModel getUserAndValidate(AuthenticationFlowContext context) {
        context.clearUser();
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        String mobileNumber = inputData.getFirst("mobile");
        UserModel user = null;
        boolean isValidMobile = false;
        if (mobileNumber != null) {
            mobileNumber = mobileNumber.trim();
            isValidMobile = MobileNumberValidator.isValid(mobileNumber);
        }

        if (!isValidMobile) {
            Response challengeResponse = this.challengeMessage(context, INVALID_MOBILE_ERROR, AuthenticationManager.FORM_USERNAME);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        } else {
            user = getUserFromMobile(mobileNumber, context.getSession());

            /** mobile not registered **/
            if (user == null || !user.isEnabled() || AuthenticationUtils.isDisabledByBruteForce(context, user)) {
                /** raise error **/
                Response challengeResponse = this.challengeMessage(context, INVALID_MOBILE_ERROR, AuthenticationManager.FORM_USERNAME);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            }
        }
        return user;
    }

    protected boolean validateOTP(AuthenticationFlowContext context, UserModel user) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        String otp = inputData.getFirst("otp");
        if (otp != null && !otp.isEmpty()) {
            otp = otp.trim();
        }

        if (user != null && user.isEnabled()) {
            if (otp == null) {
                Response challengeResponse = this.challengeMessage(context, "invalidTotpMessage", "totp");
                context.challenge(challengeResponse);
            } else {
                boolean isProdEnv = Helper.isProdEnv();
                if (!isProdEnv) {
                    if (otp.equals("123456")) {
                        return true;
                    }
                }
                OTPValidator otpValidator = new OTPValidatorService(context, user);
                try {
                    boolean valid = otpValidator.isValid(otp);
                    if (valid) {
                        return true;
                    }
                } catch (Exception e) {
                    Response challengeResponse = this.challengeMessage(context, "invalidOtpCredentials", "totp");
                    context.failureChallenge(AuthenticationFlowError.CREDENTIAL_SETUP_REQUIRED, challengeResponse);
                    return false;
                }
                Response challengeResponse = this.challengeMessage(context, "invalidTotpMessage", "totp");
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            }
        }
        return false;
    }


    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {

    }

    protected Response challengeMessage(AuthenticationFlowContext context, String error, String field) {

        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        if (error != null) {
            if (field != null) {
                form.addError(new FormMessage(field, error));
            } else {
                form.setError(error, new Object[0]);
            }
        }

        return form.createForm(TPL_CODE);
    }

}
