package com.dehaat.rest;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.common.Helper;
import com.dehaat.common.HoneybadgerErrorReporter;
import com.dehaat.common.MobileNumberValidator;
import com.dehaat.service.OTPGeneratorService;
import com.dehaat.service.OTPGenerator;
import com.dehaat.service.SMSSender;
import com.dehaat.service.SendOTPService;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.json.JSONObject;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UsersResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author sushil
 **/
public class CustomRestEndPoints {
    private final KeycloakSession session;
    private final AuthenticationManager.AuthResult auth;
    private static final String MOBILE = "mobile_number";
    private static final String ERR_INVALID_MOBILE = "Invalid Mobile Number";
    private static final String ERR_EMPTY_EMAIL_OR_MOBILE = "Email or Mobile Number missing/invalid";
    private static final String ERR_EMPTY_MOBILE = "mobile_number is missing";
    private static final String SENT_SMS_MOBILE_SUCCESS = "sms service successfully sent otp";
    private static final String SENT_SMS_MOBILE_FAIL = "sms service failed to send otp";

    public CustomRestEndPoints(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    }

    @POST
    @Path("sendOTP")
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
            user = AuthenticationUtils.createUser(session);
            AuthenticationUtils.generateSecret(user.getId(), session);
            user.setSingleAttribute(MOBILE, mobile_number);
            user.setEnabled(true);

            UserRepresentation userRepresentation = ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), user);
            JSONObject jsonObj = new JSONObject(userRepresentation);
            JSONObject message = Helper.setMessageQueueData(OperationType.CREATE.name(), jsonObj);
            Helper.getMessagingQueueService().send(message);
        }

        if (user != null && user.isEnabled() && AuthenticatorUtils.getDisabledByBruteForceEventError((BruteForceProtector)this.session.getProvider(BruteForceProtector.class), session, session.getContext().getRealm(),user)==null) {

            // get config details from server
            AuthenticatorConfigModel config = session.getContext().getRealm().getAuthenticatorConfigByAlias("mobile_otp_config");
            int ttl = Integer.parseInt(config.getConfig().get("ttl"));
            String mobileNumber = user.getFirstAttribute(MOBILE);
            String otp;

            OTPGenerator otpGenerator = new OTPGeneratorService(session, user);
            try {
                otp = otpGenerator.createOTP();
            } catch (Exception ex) {
                return Response.serverError().entity(ex.getMessage()).build();
            }
            SMSSender sender = new SendOTPService(mobileNumber, otp, ttl, client_id);
            boolean isMailSent = sender.send();
            if (isMailSent) {
                // 200 on success
                return Response.ok().entity(SENT_SMS_MOBILE_SUCCESS).build();
            }
        }
        // 500 on failure
        HoneybadgerErrorReporter.getReporter().reportError(new Exception(SENT_SMS_MOBILE_FAIL));
        return Response.serverError().entity(SENT_SMS_MOBILE_FAIL).build();
    }

    @GET
    @NoCache
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserRepresentation> getUserByQuery(@QueryParam(MOBILE) String mobileNumber) {
        if (auth == null) {
            throw new NotAuthorizedException("Admin token is not provided");
        }
        AccessToken token = auth.getToken();
        AdminAuth auth = authenticateRealmAdminRequest(token);
        RealmModel realm = auth.getRealm();

        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);

        try {
            realmAuth.users().requireQuery();
        } catch (ForbiddenException var11) {
            throw var11;
        }

        if (mobileNumber == null) {
            throw new ClientErrorException(ERR_EMPTY_MOBILE, Response.Status.BAD_REQUEST);
        }

        boolean isValidMobile = MobileNumberValidator.isValid(mobileNumber);

        if (!isValidMobile) {
            throw new ClientErrorException(ERR_INVALID_MOBILE, Response.Status.BAD_REQUEST);
        }

        List<UserRepresentation> userStream = session
                .users()
                .searchForUserByUserAttributeStream(realm, MOBILE, mobileNumber)
                .map(userModel -> ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), userModel))
                .collect(Collectors.toList());
        return userStream;
    }


    @POST
    @Path("users")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(UserRepresentation userRepresentation) {
        if (auth == null) {
            throw new NotAuthorizedException("Admin token is not provided");
        }
        AccessToken token = auth.getToken();
        AdminAuth auth = authenticateRealmAdminRequest(token);
        RealmModel realm = auth.getRealm();

        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);

        try {
            realmAuth.users().requireManage();
        } catch (ForbiddenException var11) {
            throw var11;
        }

        boolean isEmptyEmail = userRepresentation.getEmail() == null ? true : false;
        boolean isValidMobile = false;

        Map<String, List<String>> userAttributes = userRepresentation.getAttributes();
        List<String> mobileNumberList = null;
        if (userAttributes != null) {
            mobileNumberList = userAttributes.get(MOBILE);
        }

        if (mobileNumberList != null && mobileNumberList.size() > 0) {
            String mobileNumber = mobileNumberList.get(0);
            isValidMobile = MobileNumberValidator.isValid(mobileNumber);
            if (!isValidMobile) {
                return ErrorResponse.error(ERR_INVALID_MOBILE, Response.Status.BAD_REQUEST);
            }
        }

        /** check occurance of atleast one mandatory parameter [email or mobile_number] **/
        if (isEmptyEmail && !isValidMobile) {
            return ErrorResponse.error(ERR_EMPTY_EMAIL_OR_MOBILE, Response.Status.BAD_REQUEST);
        }

        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, session.getContext().getConnection());
        String username;
        username = userRepresentation.getUsername();
        if (username == null || username.isEmpty()) {
            username = KeycloakModelUtils.generateId();
            userRepresentation.setUsername(username);
        }
        UsersResource usersResource = new UsersResource(realm, realmAuth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(usersResource);
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
    }
}

