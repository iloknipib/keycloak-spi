package com.dehaat.rest;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.service.GenerateOTPService;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author sushil
 **/
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

        Stream<UserModel> userStream = session.users().searchForUserByUserAttributeStream(session.getContext().getRealm(), "mobile_number", mobile_num);
        List<UserModel> usersList = userStream.collect(Collectors.toList());

        UserModel user;
        boolean isOTPSent = false;

        if (usersList.size() > 0) {
            /** mobile already registered **/
            user = usersList.get(0);
        } else {
            /** create new user **/
            MultivaluedMap<String, String> inputData = new MultivaluedMapImpl<>();
            user = AuthenticationUtils.createUser(inputData, session);
            AuthenticationUtils.generateSecret(user.getId(), session);
            user.setSingleAttribute("mobile_number",mobile_num);
            user.setEnabled(true);
        }

        if (user != null && user.isEnabled()) {
            isOTPSent = new GenerateOTPService(session).generateOTPAndSend("HmacSHA1", 6, 300, 1, user);
        }
    }
}
