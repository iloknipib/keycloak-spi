package com.dehaat.rest;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.common.MobileNumberValidator;
import com.dehaat.service.GenerateOTPService;
import com.dehaat.service.OTPGenerator;
import com.dehaat.service.SMSSender;
import com.dehaat.service.SendOTPService;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.models.*;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendOTP(OTPPayloadRepresentation rep) {
        String mobile_number = rep.getMobile_number();
        String client_id = rep.getClient_id();

        if (mobile_number == null) {
            return ErrorResponse.error("mobile_number is missing", Response.Status.BAD_REQUEST);
        }

        if (client_id == null) {
            return ErrorResponse.error("client_id is missing", Response.Status.BAD_REQUEST);
        }

        boolean isValidMobile = MobileNumberValidator.isValid(mobile_number);
        if (!isValidMobile) {
            return ErrorResponse.error("Invalid Mobile Number", Response.Status.BAD_REQUEST);
        }

        Stream<UserModel> userStream = session.users().searchForUserByUserAttributeStream(session.getContext().getRealm(), "mobile_number", mobile_number);
        List<UserModel> usersList = userStream.collect(Collectors.toList());

        UserModel user;

        if (usersList.size() > 0) {
            /** mobile already registered **/
            user = usersList.get(0);
        } else {
            /** create new user **/
            MultivaluedMap<String, String> inputData = new MultivaluedMapImpl<>();
            user = AuthenticationUtils.createUser(inputData, session);
            AuthenticationUtils.generateSecret(user.getId(), session);
            user.setSingleAttribute("mobile_number", mobile_number);
            user.setEnabled(true);
        }

        if (user != null && user.isEnabled()) {

            // get config details from server
            AuthenticatorConfigModel config = session.getContext().getRealm().getAuthenticatorConfigByAlias("mobile_otp_config");
            int ttl = Integer.parseInt(config.getConfig().get("ttl"));
            int length = Integer.parseInt(config.getConfig().get("length"));
            String senderServiceURL = config.getConfig().get("SenderServiceURL");
            String token = config.getConfig().get("token");

            OTPGenerator otpGenerator = new GenerateOTPService(session, user, "HmacSHA1", length, ttl, 1);
            String otp = otpGenerator.createOTP();
            System.out.println(otp);
            SMSSender sender = new SendOTPService(senderServiceURL, user.getFirstAttribute("mobile_number"), otp, ttl, token);
            boolean isMailSent = sender.send();
            if (isMailSent) {
                // 200 on success
                return Response.ok().type(MediaType.APPLICATION_JSON).build();
            }
        }
        // 500 on failure
        return Response.serverError().type(MediaType.APPLICATION_JSON).build();
    }


    /** In development
     @POST
     @Path("users")
     @NoCache
     @Consumes(MediaType.APPLICATION_JSON) public Response createUser(UserRepresentation userRepresentation) {
     AccessToken token = auth.getToken();
     AdminAuth auth = authenticateRealmAdminRequest(token);
     RealmModel realm = auth.getRealm();
     AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);

     AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, session.getContext().getConnection());

     String username;
     username = userRepresentation.getUsername();
     if (username == null || username.isEmpty()) {
     username = KeycloakModelUtils.generateId();
     userRepresentation.setUsername(username);
     }

     UsersResource usersResource = new UsersResource(realm, realmAuth, adminEvent);
     return usersResource.createUser(userRepresentation);
     }


     protected AdminAuth authenticateRealmAdminRequest(AccessToken token) {
     String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
     RealmManager realmManager = new RealmManager(session);
     RealmModel realm = realmManager.getRealmByName(realmName);
     if (realm == null) {
     throw new NotAuthorizedException("Unknown realm in token");
     }
     session.getContext().setRealm(realm);


     AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
     .setRealm(realm)
     .authenticate();

     if (authResult == null) {
     throw new NotAuthorizedException("Bearer");
     }

     ClientModel client = realm.getClientByClientId(token.getIssuedFor());
     if (client == null) {
     throw new NotFoundException("Could not find client for authorization");

     }

     return new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client);
     } **/
}

