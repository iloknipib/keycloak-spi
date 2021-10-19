package com.dehaat.rest;

/**
 * @author sushil
 **/
public class OTPPayloadRepresentation {

    private String mobile_num;
    private String client_id;

    public String getMobile_num() {
        return mobile_num;
    }

    public void setMobile_num(String mobile_num) {
        this.mobile_num = mobile_num;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public OTPPayloadRepresentation() {
    }
}
