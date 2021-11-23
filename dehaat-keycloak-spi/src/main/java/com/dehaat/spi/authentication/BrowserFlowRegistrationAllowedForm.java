package com.dehaat.spi.authentication;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.common.Helper;
import com.dehaat.common.MobileNumberValidator;
import org.json.JSONObject;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.events.admin.OperationType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author sushil
 */
public class BrowserFlowRegistrationAllowedForm extends UsernamePasswordForm {
    private static final String TPL_CODE = "login-browser.ftl";
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
        return this.validateUser(context, formData);
    }


    public boolean validateUser(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        context.clearUser();
        String mobileNumber = inputData.getFirst(AuthenticationManager.FORM_USERNAME).trim();
        UserModel user = null;
        boolean isValidMobile = MobileNumberValidator.isValid(mobileNumber);
        if (!isValidMobile) {
            context.challenge(challengeMessage(context, INVALID_MOBILE_ERROR, AuthenticationManager.FORM_USERNAME));

        } else {
            Stream<UserModel> userStream = context.getSession().users().searchForUserByUserAttributeStream(context.getRealm(), MOBILE_NUMBER, mobileNumber);
            List<UserModel> usersList = userStream.collect(Collectors.toList());

            /** mobile already registered **/
            if (usersList.size() > 0) {
                user = usersList.get(0);
                context.setUser(user);
                context.success();
            } else {
                /** create new user **/
                user = AuthenticationUtils.createUser(inputData, context.getSession());
                AuthenticationUtils.generateSecret(user.getId(), context.getSession());
                user.setSingleAttribute(MOBILE_NUMBER, mobileNumber);
                user.setEnabled(true);
                context.setUser(user);
                context.success();

                /** send message to the messaging queue ***/
                UserRepresentation userRepresentation = ModelToRepresentation.toRepresentation(context.getSession(), context.getRealm(), user);
                JSONObject jsonObj = new JSONObject(userRepresentation);
                JSONObject message = Helper.setMessageQueueData(OperationType.CREATE.name(), jsonObj);
                Helper.getMessagingQueueService().send(message);
            }
        }
        return user != null && user.isEnabled();
    }


    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        LoginFormsProvider forms = context.form();
        if (!formData.isEmpty()) {
            forms.setFormData(formData);
        }

        return forms.createLoginUsername();
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

