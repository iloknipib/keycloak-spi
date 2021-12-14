package com.dehaat.spi.authentication;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.common.MobileNumberValidator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static com.dehaat.common.AuthenticationUtils.getUserFromMobile;

/**
 * @author sushil
 */
public class BrowserFlowRegistrationNotAllowedForm extends UsernamePasswordForm {
    private static final String TPL_CODE = "login-browser.ftl";
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
        return this.validateUser(context);
    }

    public boolean validateUser(AuthenticationFlowContext context) {
        context.clearUser();
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        String mobileNumber = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
        UserModel user;
        boolean isValidMobile = false;
        if (mobileNumber != null) {
            mobileNumber = mobileNumber.trim();
            isValidMobile = MobileNumberValidator.isValid(mobileNumber);
        }
        if (!isValidMobile) {
            context.challenge(challengeMessage(context, INVALID_MOBILE_ERROR, AuthenticationManager.FORM_USERNAME));

        } else {
            user = getUserFromMobile(mobileNumber, context.getSession());

            /** mobile already registered **/
            if (user != null && user.isEnabled() && !AuthenticationUtils.isDisabledByBruteForce(context, user)) {
                return true;
            } else {
                /** raise error **/
                Response challengeResponse = this.challengeMessage(context, INVALID_MOBILE_ERROR, AuthenticationManager.FORM_USERNAME);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            }
        }
        return false;
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

