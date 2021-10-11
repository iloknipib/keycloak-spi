package com.dehaat.spi.authentication;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.jpa.DehaatUserMobileEntity;
import com.dehaat.service.DehaatUserMobileEntityService;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author sushil
 */
public class MobileForm extends UsernamePasswordForm {
    public MobileForm() {
    }

    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        return this.validateUser(context, formData);
    }


    public boolean validateUser(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        context.clearUser();
        String mobileNumber = inputData.getFirst(AuthenticationManager.FORM_USERNAME).trim();
        UserModel user = null;
        if (mobileNumber.length() != 10) {
            context.challenge(challengeMessage(context, "Invalid Mobile Number", "username"));

        } else {
            context.getSession().setAttribute("mobile_number", mobileNumber);
            EntityManager em = context.getSession().getProvider(JpaConnectionProvider.class).getEntityManager();
            String realm = context.getRealm().getName();
            String userId = DehaatUserMobileEntityService.getUserIdByMobile(em, mobileNumber, realm);


            /** mobile already registered **/
            if (userId != null && !userId.isEmpty()) {
                List<UserModel> usersList = AuthenticationUtils.getUserEntityByUserId(em, userId, context.getRealm(), context.getSession());
                if (usersList.size() > 0) {
                    user = usersList.get(0);
                    context.setUser(user);
                    context.success();
                }
            } else {
                /** create new user **/
                user = AuthenticationUtils.createUser(inputData, context.getSession());
                AuthenticationUtils.generateSecret(user.getId(), context.getSession());
                DehaatUserMobileEntityService.createUserMobileEntity(em, mobileNumber, user.getId(), context.getRealm().getName());
                user.setEnabled(true);
                context.setUser(user);
                context.success();
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

    protected Response createLoginForm(LoginFormsProvider form) {
        return form.createLoginUsername();
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

        return form.createLoginUsername();
    }

}
