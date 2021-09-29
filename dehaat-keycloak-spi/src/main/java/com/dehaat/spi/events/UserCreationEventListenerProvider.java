
package com.dehaat.spi.events;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;


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
            generateSecret(userID);
        }


    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

        /** create secret for totp when event type is user creation from admin/admin-api **/
        if (ResourceType.USER.equals(adminEvent.getResourceType())
                && OperationType.CREATE.equals(adminEvent.getOperationType())) {
            String userID = adminEvent.getResourcePath().replaceAll("users", "").replaceAll("/", "");
            generateSecret(userID);
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }

    protected void generateSecret(String userID) {

        UserModel user = session.users().getUserById(session.getContext().getRealm(), userID);
        RealmModel realm = session.getContext().getRealm();
        CredentialProvider otpCredentialProvider = session.getProvider(CredentialProvider.class, "keycloak-otp");
        OTPCredentialModel credentialModel = OTPCredentialModel.createFromPolicy(realm, HmacOTP.generateSecret(20));

        // create credentials
        CredentialModel createdCredential = otpCredentialProvider.createCredential(realm, user, credentialModel);
        System.out.println(createdCredential.getId());

    }

}
