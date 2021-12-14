package com.dehaat.spi.authentication;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VerifyEmailForm implements Authenticator {

    private static final String TPL_CODE = "custom-login-verify-email.ftl";


    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getUser().getEmail() != null && !context.getUser().isEmailVerified()) {
            context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
            LoginFormsProvider loginFormsProvider = context.form();
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            authSession.setClientNote("APP_INITIATED_FLOW", (String) null);

            String email = context.getUser().getEmail();
            if (!Objects.equals(authSession.getAuthNote("VERIFY_EMAIL_KEY"), email)) {
                authSession.setAuthNote("VERIFY_EMAIL_KEY", email);
                EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail("email", email);
                this.sendVerifyEmail(context.getSession(), loginFormsProvider, context.getUser(), context.getAuthenticationSession(), event);
            }

            context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));

        } else {
            context.success();
        }
    }

    private void sendVerifyEmail(KeycloakSession session, LoginFormsProvider forms, UserModel user, AuthenticationSessionModel authSession, EventBuilder event) throws UriBuilderException, IllegalArgumentException {
        RealmModel realm = session.getContext().getRealm();
        UriInfo uriInfo = session.getContext().getUri();
        int validityInSecs = realm.getActionTokenGeneratedByUserLifespan("verify-email");
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
        String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
        VerifyEmailActionToken token = new VerifyEmailActionToken(user.getId(), absoluteExpirationInSecs, authSessionEncodedId, user.getEmail(), authSession.getClient().getClientId());
        UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo), authSession.getClient().getClientId(), authSession.getTabId());
        String link = builder.build(new Object[]{realm.getName()}).toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes((long) validityInSecs);

        try {
            ((EmailTemplateProvider) session.getProvider(EmailTemplateProvider.class)).setAuthenticationSession(authSession).setRealm(realm).setUser(user).sendVerifyEmail(link, expirationInMinutes);
            event.success();
        } catch (EmailException var17) {
            event.error("email_send_failed");
        }
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
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
}
