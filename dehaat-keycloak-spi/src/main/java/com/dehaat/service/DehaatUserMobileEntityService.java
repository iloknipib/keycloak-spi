package com.dehaat.service;

import com.dehaat.jpa.DehaatUserMobileEntity;
import org.hibernate.HibernateException;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class DehaatUserMobileEntityService {

    /***
     *  This class is meant to expose api related to DehaatUserMobileEntity
     *
     * */

    private static final String EMPTY_STRING = "";

    public static String getUserIdByMobile(EntityManager em, String mobileNumber, String realmId) {
        if (mobileNumber == null || mobileNumber.isEmpty() || realmId == null || realmId.isEmpty()) {
            return EMPTY_STRING;
        }
        TypedQuery<String> query = em.createNamedQuery("getUserIdFromMobile", String.class);
        List<String> userIdList = query.setParameter("mobile", mobileNumber)
                .setParameter("realmId", realmId)
                .getResultList();

        if (userIdList.size() > 0) {
            return userIdList.get(0);
        }
        return EMPTY_STRING;
    }

    public static DehaatUserMobileEntity getDehaatUserMobileEntityByUserId(EntityManager em, String userId, String realm) {
        TypedQuery<DehaatUserMobileEntity> querySelect = em.createNamedQuery("getMobileInfoFromUser", DehaatUserMobileEntity.class);
        DehaatUserMobileEntity userMobileInfo = querySelect.setParameter("realmId", realm)
                .setParameter("userId", userId)
                .getSingleResult();
        return userMobileInfo;
    }

    public static void createUserMobileEntity(EntityManager em, String mobileNumber, String userId, String realm) {
        DehaatUserMobileEntity UserMobileEntity = new DehaatUserMobileEntity();
        UserMobileEntity.setId(KeycloakModelUtils.generateId());
        UserMobileEntity.setMobile(mobileNumber);
        UserMobileEntity.setUserId(userId);
        UserMobileEntity.setRealmId(realm);
        UserMobileEntity.setIs_verified(false);
        em.getTransaction().begin();
        try{
            em.persist(UserMobileEntity);
        }catch (HibernateException ex){
            throw ex;
        }
        em.getTransaction().commit();
    }

    public static int updateMobileVerifiedFlag(EntityManager em, String mobileNumber, String realm) {
        return em.createNamedQuery("updateMobileVerifiedFlag")
                .setParameter("is_verified", true)
                .setParameter("verified_at", System.currentTimeMillis())
                .setParameter("mobile", mobileNumber)
                .setParameter("realmId", realm)
                .executeUpdate();
    }
}

