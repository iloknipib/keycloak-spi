package com.dehaat.rest;

import org.keycloak.representations.idm.UserRepresentation;

public class CustomUserRepresentation {

    String mobile;
    UserRepresentation userinfo;


    public UserRepresentation getUserinfo() {
        return userinfo;
    }

    public void setUserinfo(UserRepresentation userinfo) {
        this.userinfo = userinfo;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
