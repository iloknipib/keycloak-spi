
package com.dehaat.spi.events;

import com.dehaat.common.AuthenticationUtils;
import org.jboss.logging.Logger;
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
public class UserCreationEventListenerProvider implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(UserCreationEventListenerProvider.class);

    private final KeycloakSession session;

    public UserCreationEventListenerProvider(KeycloakSession session) {
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

        /** create secret for totp when event type is user creation from admin/admin-api **/
        if (ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.CREATE.equals(adminEvent.getOperationType())) {
            String resources [] = adminEvent.getResourcePath().split("/");
            String userID = resources[resources.length-1];
            AuthenticationUtils.generateSecret(userID, session);
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }

}
