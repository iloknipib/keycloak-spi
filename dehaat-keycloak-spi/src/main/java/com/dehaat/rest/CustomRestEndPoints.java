package com.dehaat.rest;

import com.dehaat.common.AuthenticationUtils;
import com.dehaat.service.DehaatUserMobileEntityService;
import com.dehaat.service.GenerateOTPService;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.policy.PasswordPolicyNotMetException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.UsersResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.keycloak.userprofile.UserProfileContext.USER_API;

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


    @POST
    @Path("users")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void createUser(CustomUserRepresentation userRepresentation) {
        AccessToken token = auth.getToken();
        AdminAuth auth = authenticateRealmAdminRequest(token);
        RealmModel realm = auth.getRealm();
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session,realm,auth);

        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session,session.getContext().getConnection() );

        String username;
        username = userRepresentation.getUserinfo().getUsername();
        if(username==null || username.isEmpty()){
            username = KeycloakModelUtils.generateId();
            userRepresentation.getUserinfo().setUsername(username);
        }

        UserModel user = createUser(userRepresentation.userinfo,realm,realmAuth,adminEvent);
        user.setEnabled(true);
//        UserModel user = session.users().getUserByUsername(realm,username);
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        DehaatUserMobileEntityService.createUserMobileEntity(em, userRepresentation.getMobile(),user.getId(),realm.getName());
    }


    private void checkRealmAdmin() {
        if (auth == null) {
            throw new NotAuthorizedException("Bearer");
        } else if (auth.getToken().getRealmAccess() == null || !auth.getToken().getRealmAccess().isUserInRole("admin")) {
            throw new ForbiddenException("Does not have realm admin role");
        }
    }


    public UserModel createUser(final UserRepresentation rep,RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        // first check if user has manage rights
        try {
            auth.users().requireManage();
        }
        catch (org.keycloak.services.ForbiddenException exception) {
            // if user does not have manage rights, fallback to fine grain admin permissions per group
            if (rep.getGroups() != null) {
                // if groups is part of the user rep, check if admin has manage_members and manage_membership on each group
                for (String groupPath : rep.getGroups()) {
                    GroupModel group = KeycloakModelUtils.findGroupByPath(realm, groupPath);
                    if (group != null) {
                        auth.groups().requireManageMembers(group);
                        auth.groups().requireManageMembership(group);
                    } else {
//                        return ErrorResponse.error(String.format("Group %s not found", groupPath), Response.Status.BAD_REQUEST);
                    }
                }
            } else {
                // propagate exception if no group specified
                throw exception;
            }
        }

        String username = rep.getUsername();
        UserModel user=null;
        if(realm.isRegistrationEmailAsUsername()) {
            username = rep.getEmail();
        }
        if (ObjectUtil.isBlank(username)) {
//            return ErrorResponse.error("User name is missing", Response.Status.BAD_REQUEST);
        }

        // Double-check duplicated username and email here due to federation
        if (session.users().getUserByUsername(realm, username) != null) {
//            return ErrorResponse.exists("User exists with same username");
        }
        if (rep.getEmail() != null && !realm.isDuplicateEmailsAllowed()) {
            try {
                if(session.users().getUserByEmail(realm, rep.getEmail()) != null) {
//                    return ErrorResponse.exists("User exists with same email");
                }
            } catch (ModelDuplicateException e) {
//                return ErrorResponse.exists("User exists with same email");
            }
        }

        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);

        UserProfile profile = profileProvider.create(USER_API, rep.toAttributes());

        try {
            Response response = UserResource.validateUserProfile(profile, null, session);
            if (response != null) {
//                return response;
            }

            user = profile.create();

            UserResource.updateUserFromRep(profile, user, rep, session, false);
            RepresentationToModel.createFederatedIdentities(rep, session, realm, user);
            RepresentationToModel.createGroups(rep, realm, user);

            RepresentationToModel.createCredentials(rep, session, realm, user, true);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();

            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }

//            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
//            return ErrorResponse.exists("User exists with same username or email");
        } catch (PasswordPolicyNotMetException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
//            return ErrorResponse.error("Password policy not met", Response.Status.BAD_REQUEST);
        } catch (ModelException me){
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
//            return ErrorResponse.error("Could not create user", Response.Status.BAD_REQUEST);
        }
        return user;
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
