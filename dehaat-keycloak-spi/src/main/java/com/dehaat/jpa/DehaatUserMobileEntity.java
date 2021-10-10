package com.dehaat.jpa;

import javax.persistence.*;

@NamedQuery(name = "getUserFromMobile", query = "select attr.id from DehaatUserMobileEntity attr where attr.mobile=:mobile and attr.realmId=:realmId")
@NamedQuery(name = "getMobileInfoFromUser", query = "select attr from DehaatUserMobileEntity attr where attr.userId=:userId and attr.realmId=:realmId")
@NamedQuery(name = "getUserIdFromMobile", query = "select attr.userId from DehaatUserMobileEntity attr where attr.mobile=:mobile and attr.realmId=:realmId")
@NamedQuery(name = "updateMobileVerifiedFlag", query = "update DehaatUserMobileEntity attr set attr.is_verified=:is_verified,attr.verified_at=:verified_at where attr.mobile=:mobile and attr.realmId=:realmId")


@Entity
@Table(name = "DEHAAT_USER_MOBILE_ENTITY")
public class DehaatUserMobileEntity {

    @Id
    @Column(name = "ID")
    @Access(AccessType.PROPERTY)
    // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "USER_ID")
    protected String userId;

    @Column(name = "MOBILE", nullable = false)
    protected String mobile;

    @Column(name = "REALM_ID", nullable = false)
    protected String realmId;

    @Column(name = "IS_VERIFIED", nullable = false)
    protected boolean is_verified;

    @Column(name = "VERIFIED_AT", nullable = true)
    protected long verified_at;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public boolean isIs_verified() {
        return is_verified;
    }

    public void setIs_verified(boolean is_verified) {
        this.is_verified = is_verified;
    }

    public long getVerified_at() {
        return verified_at;
    }

    public void setVerified_at(long verified_at) {
        this.verified_at = verified_at;
    }

}
