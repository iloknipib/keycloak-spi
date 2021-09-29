package com.dehaat.rest;

import com.dehaat.service.GenerateOTPService;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SendOTPRestResource {


    private final KeycloakSession session;
    private final AuthenticationManager.AuthResult auth;

    public SendOTPRestResource(KeycloakSession session) {
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

        Stream<UserModel> userStream = session.users().searchForUserByUserAttributeStream(session.getContext().getRealm(), "mobile_number", mobile_num);
        List<UserModel> usersList = userStream.collect(Collectors.toList());

        UserModel user;
        boolean isOTPSent = false;

        if (usersList.size() > 0) {
            user = usersList.get(0);
            if (user != null && user.isEnabled()) {
                isOTPSent = new GenerateOTPService(session).generateOTPAndSend("HmacSHA1", 6, 60, 1, user);
            }

        } else {
            // handle invalid user
        }
    }

}
