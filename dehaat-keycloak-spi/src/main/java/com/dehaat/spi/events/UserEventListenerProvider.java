
package com.dehaat.spi.events;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.common.Helper;
import org.jboss.logging.Logger;
import org.json.JSONObject;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author sushil
 **/
public class UserEventListenerProvider implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(UserEventListenerProvider.class);

    private final KeycloakSession session;

    public UserEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }


    @Override
    public void onEvent(Event event) {

        /** create secret for totp when event type is register **/
        if (event.getType().equals(EventType.REGISTER)) {
            String userID = event.getUserId();
            AuthenticationUtils.generateSecret(userID, session);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

        /** Handling create user event **/
        if (ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.CREATE.equals(adminEvent.getOperationType())) {
            String resources[] = adminEvent.getResourcePath().split("/");
            String userID = resources[resources.length - 1];
            AuthenticationUtils.generateSecret(userID, session);

            /** Send reset password email if required actions contains UPDATE_PASSWORD **/
            String clientId = adminEvent.getAuthDetails().getClientId();
            UserModel user = session.users().getUserById(session.getContext().getRealm(), userID);
            if (user.getEmail() != null && user.isEnabled() && user.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_PASSWORD.name())) {
                sendResetPasswordEmail(user, clientId);
            }

            /** send create even to messaging queue **/
            JSONObject jsonObj = new JSONObject(adminEvent.getRepresentation());
            if (!jsonObj.has("emailVerified")) {
                jsonObj.put("emailVerified", false);
            }
            if (!jsonObj.has("id")) {
                jsonObj.put("id", userID);
            }
            JSONObject message = Helper.setMessageQueueData(OperationType.CREATE.name(), jsonObj);
            Helper.getMessagingQueueService().send(message);
        }

        /** Handling update user event **/
        if (ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.UPDATE.equals(adminEvent.getOperationType())) {

            JSONObject jsonObj = new JSONObject(adminEvent.getRepresentation());
            JSONObject message = Helper.setMessageQueueData(OperationType.UPDATE.name(), jsonObj);
            Helper.getMessagingQueueService().send(message);
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }


    private void sendResetPasswordEmail(UserModel user, String clientId) {
        List<String> actions = new LinkedList();
        String redirectUri = null;
        ClientModel client = this.session.getContext().getRealm().getClientById(clientId);
        if (client.getRedirectUris().size() > 0) {
            redirectUri = client.getRedirectUris().iterator().next();
        }
        executeActionsEmail(user, redirectUri, client.getClientId(), actions);
    }


    private Response executeActionsEmail(UserModel user, String redirectUri, String clientId, List<String> actions) {
        if (user.getEmail() == null) {
            return ErrorResponse.error("User email missing", Response.Status.BAD_REQUEST);
        } else if (!user.isEnabled()) {
            throw new WebApplicationException(ErrorResponse.error("User is disabled", Response.Status.BAD_REQUEST));
        } else if (redirectUri != null && clientId == null) {
            throw new WebApplicationException(ErrorResponse.error("Client id missing", Response.Status.BAD_REQUEST));
        } else {
            if (clientId == null) {
                clientId = "account";
            }

            ClientModel client = this.session.getContext().getRealm().getClientByClientId(clientId);
            if (client == null) {
                throw new WebApplicationException(ErrorResponse.error("Client doesn't exist", Response.Status.BAD_REQUEST));
            } else if (!client.isEnabled()) {
                throw new WebApplicationException(ErrorResponse.error("Client is not enabled", Response.Status.BAD_REQUEST));
            } else {
                if (redirectUri != null) {
                    String redirect = RedirectUtils.verifyRedirectUri(this.session, redirectUri, client);
                    if (redirect == null) {
                        throw new WebApplicationException(ErrorResponse.error("Invalid redirect uri.", Response.Status.BAD_REQUEST));
                    }
                }

                RealmModel realm = this.session.getContext().getRealm();
                Integer lifespan = realm.getActionTokenGeneratedByAdminLifespan();


                int expiration = Time.currentTime() + lifespan;
                ExecuteActionsActionToken token = new ExecuteActionsActionToken(user.getId(), user.getEmail(), expiration, actions, redirectUri, clientId);

                try {
                    UriBuilder builder = LoginActionsService.actionTokenProcessor(this.session.getContext().getUri());
                    builder.queryParam("key", new Object[]{token.serialize(this.session, realm, this.session.getContext().getUri())});
                    String link = builder.build(new Object[]{realm.getName()}).toString();
                    ((EmailTemplateProvider) this.session.getProvider(EmailTemplateProvider.class)).setAttribute("requiredActions", token.getRequiredActions()).setRealm(realm).setUser(user).sendExecuteActions(link, TimeUnit.SECONDS.toMinutes((long) lifespan));
                    return Response.noContent().build();
                } catch (EmailException var11) {
                    ServicesLogger.LOGGER.failedToSendActionsEmail(var11);
                    return ErrorResponse.error("Failed to send execute actions email", Response.Status.INTERNAL_SERVER_ERROR);
                }
            }
        }
    }


}
