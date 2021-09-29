package com.dehaat.spi.authentication;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AndroidLoginForm extends OTPFormAuthenticator {

    private static final String TPL_CODE = "login-android.ftl";

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
        return this.validateUser(context, formData) && this.validateOTP(context,formData);
    }

    public boolean validateUser(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        context.clearUser();
        String mobile_num = inputData.getFirst("mobile").trim();
        UserModel user = null;
        if (mobile_num.length() != 10) {
            challengeMessage(context, "Invalid Mobile Number","username");
        }else {
            Stream<UserModel> userStream = context.getSession().users().searchForUserByUserAttributeStream(context.getRealm(), "mobile_number", mobile_num);
            List<UserModel> usersList = userStream.collect(Collectors.toList());
            if (usersList.size() > 0) {
                user = usersList.get(0);
                context.setUser(user);
                context.success();
            }else{
                challengeMessage(context, "Invalid Mobile Number","username");
            }
        }
        return user!=null && user.isEnabled();
    }

    public boolean validateOTP(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData){
        String otp = inputData.getFirst("otp").trim();
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        int ttl = Integer.parseInt(config.getConfig().get("ttl"));
        int length = Integer.parseInt(config.getConfig().get("length"));
        OTPCredentialModel defaultOtpCredential = this.getCredentialProvider(context.getSession()).getDefaultCredential(context.getSession(), context.getRealm(), context.getUser());
        TimeBasedOTP timeBasedOTP =  new TimeBasedOTP("HmacSHA1",length , ttl, 1);
        String secretData = defaultOtpCredential.getSecretData();

        UserModel userModel = context.getUser();
        if (this.enabledUser(context, userModel)) {
            if (otp == null) {
                Response challengeResponse = this.challenge(context, (String)null);
                context.challenge(challengeResponse);
            } else {
                boolean valid = timeBasedOTP.validateTOTP(otp,secretData.getBytes(StandardCharsets.UTF_8));
                if (!valid) {
                    context.getEvent().user(userModel).error("invalid_user_credentials");
                    Response challengeResponse = this.challenge(context, "invalidTotpMessage", "totp");
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
                } else {
                    return true;
                }
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
