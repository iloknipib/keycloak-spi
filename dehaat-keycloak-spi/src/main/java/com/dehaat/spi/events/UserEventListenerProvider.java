
package com.dehaat.spi.events;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.common.Helper;
import org.jboss.logging.Logger;
import org.json.JSONObject;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;

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

}
