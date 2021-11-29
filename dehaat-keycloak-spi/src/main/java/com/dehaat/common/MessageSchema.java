package com.dehaat.common;


import org.json.JSONObject;

public class MessageSchema {
    private String entity;
    private String source;
    private String action;
    private JSONObject data;
    private String generated_on;

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public void setGenerated_on(String generated_on) {
        this.generated_on = generated_on;
    }

    public String getSource() {
        return source;
    }

    public String getAction() {
        return action;
    }

    public JSONObject getData() {
        return data;
    }

    public String getGenerated_on() {
        return generated_on;
    }
}
