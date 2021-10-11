package com.dehaat.rest;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.service.DehaatUserMobileEntityService;
import com.dehaat.service.GenerateOTPService;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomRestEndPoints {
    private final KeycloakSession session;
    private final AuthenticationManager.AuthResult auth;

    public CustomRestEndPoints(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    }

    @POST
    @Path("sendOTP")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void sendOTP(OTPPayloadRepresentation rep) {

        String mobile_num = rep.getMobile_num();
        String client_id = rep.getClient_id();

        if (mobile_num == null || mobile_num.length() != 10) {
            throw new ClientErrorException(400);
        }
        if (client_id == null) {
            throw new ClientErrorException(400);
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        String realm = session.getContext().getRealm().getName();
        String userId = DehaatUserMobileEntityService.getUserIdByMobile(em, mobile_num, realm);

        UserModel user = null;
        boolean isOTPSent;

        if (userId != null && !userId.isEmpty()) {
            /** mobile already registered **/

            List<UserModel> usersList = AuthenticationUtils.getUserEntityByUserId(em, userId, session.getContext().getRealm(), session);
            if (usersList.size() > 0) {
                user = usersList.get(0);
            }
        } else {
            /** create new user **/
            MultivaluedMap<String, String> inputData = new MultivaluedMapImpl<>();
            user = AuthenticationUtils.createUser(inputData, session);
            AuthenticationUtils.generateSecret(user.getId(), session);
            DehaatUserMobileEntityService.createUserMobileEntity(em, mobile_num, user.getId(), realm);
            user.setEnabled(true);
        }

        if (user != null && user.isEnabled()) {
            isOTPSent = new GenerateOTPService(session).generateOTPAndSend("HmacSHA1", 6, 60, 1, user);
        }
    }
}
