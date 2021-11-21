package com.dehaat.spi.authentication;

import com.dehaat.common.Helper;
import com.dehaat.common.MobileNumberValidator;
import com.dehaat.service.OTPValidator;
import com.dehaat.service.OTPValidatorService;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author sushil
 **/
public class AndroidLoginForm extends OTPFormAuthenticator {

    private static final String TPL_CODE = "login-android.ftl";
    private static final String INVALID_MOBILE_ERROR = "Invalid Mobile Number";
    private static final String MOBILE_NUMBER = "mobile_number";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.cancelLogin();
        } else if (this.validateForm(context, formData)) {
            context.success();
        }
    }

    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        return this.validateUser(context, formData) && this.validateOTP(context, formData);
    }

    public boolean validateUser(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        context.clearUser();
        String mobileNumber = inputData.getFirst("mobile").trim();
        UserModel user = null;
        boolean isValidMobile = MobileNumberValidator.isValid(mobileNumber);
        if (!isValidMobile) {
            challengeMessage(context, INVALID_MOBILE_ERROR, AuthenticationManager.FORM_USERNAME);
        } else {
            Stream<UserModel> userStream = context.getSession().users().searchForUserByUserAttributeStream(context.getRealm(), MOBILE_NUMBER, mobileNumber);
            List<UserModel> usersList = userStream.collect(Collectors.toList());

            /** mobile already registered **/
            if (usersList.size() > 0) {
                user = usersList.get(0);
                context.setUser(user);
                context.success();
            } else {
                /** raise error **/
                Response challengeResponse = this.challenge(context, INVALID_MOBILE_ERROR, AuthenticationManager.FORM_USERNAME);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            }
        }
        return user != null && user.isEnabled();
    }

    public boolean validateOTP(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        String otp = inputData.getFirst("otp");
        if (otp != null && !otp.isEmpty()) {
            otp = otp.trim();
        }

        UserModel userModel = context.getUser();
        if (this.enabledUser(context, userModel)) {
            if (otp == null) {
                Response challengeResponse = this.challenge(context, "invalidTotpMessage", "totp");
                context.challenge(challengeResponse);
            } else {
                boolean isProdEnv = Helper.isProdEnv();
                if (!isProdEnv) {
                    if (otp.equals("123456")) {
                        return true;
                    }
                }
                OTPValidator otpValidator = new OTPValidatorService(context);
                try {
                    boolean valid = otpValidator.isValid(otp);
                    if (valid) {
                        return true;
                    }
                }catch (Exception e) {
                    context.getEvent().user(userModel).error(e.getMessage());
                    Response challengeResponse = this.challenge(context, "invalidOtpCredentials", "totp");
                    context.failureChallenge(AuthenticationFlowError.CREDENTIAL_SETUP_REQUIRED, challengeResponse);
                    return false;
                }
                context.getEvent().user(userModel).error("invalid_user_credentials");
                Response challengeResponse = this.challenge(context, "invalidTotpMessage", "totp");
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

    protected Response createLoginForm(LoginFormsProvider form) {
        return form.createForm(TPL_CODE);
    }

}
