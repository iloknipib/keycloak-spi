package com.dehaat.rest;

/**
 * @author sushil
 **/
public class OTPPayloadRepresentation {

    public OTPPayloadRepresentation() {
    }
    
    public String getMobile_number() {
        return mobile_number;
    }

    public void setMobile_number(String mobile_number) {
        this.mobile_number = mobile_number;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    private String mobile_number;
    private String client_id;
}
