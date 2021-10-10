package com.dehaat.spi.authentication;

import com.dehaat.jpa.DehaatUserMobileEntity;
import com.dehaat.service.MailManService;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Details;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author sushil
 */
public class SmsAuthenticator extends OTPFormAuthenticator {

    private static final String TPL_CODE = "login-sms.ftl";
    public static final String SELECTED_OTP_CREDENTIAL_ID = "selectedOtpCredentialId";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        KeycloakSession session = context.getSession();

        String mobileNumber = (String) context.getSession().getAttribute("mobile_number");
        context.getSession().setAttribute("mobile_number",mobileNumber);

        if (mobileNumber != null && mobileNumber.length() == 10) {

            EntityManager em = context.getSession().getProvider(JpaConnectionProvider.class).getEntityManager();
            TypedQuery<String> query = em.createNamedQuery("getUserFromMobile", String.class);

            List<String> list = query.setParameter("mobile", mobileNumber)
                    .setParameter("realmId", context.getRealm().getName())
                    .getResultList();

            if (list.size() > 0) {

                int ttl = Integer.parseInt(config.getConfig().get("ttl"));
                int length = Integer.parseInt(config.getConfig().get("length"));


                String senderServiceURL = config.getConfig().get("SenderServiceURL");

                OTPCredentialModel defaultOtpCredential = getCredentialProvider(session)
                        .getDefaultCredential(context.getSession(), context.getRealm(), context.getUser());
                String credentialId = defaultOtpCredential == null ? "" : defaultOtpCredential.getId();
                context.getEvent().detail(Details.SELECTED_CREDENTIAL_ID, credentialId);
                context.form().setAttribute(SELECTED_OTP_CREDENTIAL_ID, credentialId);


                TimeBasedOTP timeBasedOTP = new TimeBasedOTP("HmacSHA1", length, ttl, 1);
                String code = timeBasedOTP.generateTOTP(defaultOtpCredential.getSecretData());
                System.out.println(code);

                try {

                    /*** createMailManRequest(senderServiceURL, mobileNumber, code, ttl); ***/

                    boolean isMailSent = MailManService.createMailManRequest(senderServiceURL, mobileNumber, code, ttl);
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
                    EntityManager em = context.getSession().getProvider(JpaConnectionProvider.class).getEntityManager();
                    TypedQuery<DehaatUserMobileEntity> querySelect = em.createNamedQuery("getMobileInfoFromUser", DehaatUserMobileEntity.class);
                    DehaatUserMobileEntity userMobileInfo = querySelect.setParameter("realmId", context.getRealm().getName())
                            .setParameter("userId",userModel.getId())
                            .getSingleResult();

                    if(!userMobileInfo.isIs_verified()){
                        em.createNamedQuery("updateMobileVerifiedFlag")
                                .setParameter("is_verified",true)
                                .setParameter("verified_at",System.currentTimeMillis())
                                .setParameter("mobile", userMobileInfo.getMobile())
                                .setParameter("realmId", context.getRealm().getName())
                                .executeUpdate();
                    }
                    context.success();
                }
            }
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.getFirstAttribute("mobile_number") != null;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

}