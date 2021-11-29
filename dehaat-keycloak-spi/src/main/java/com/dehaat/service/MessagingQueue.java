package com.dehaat.service;

import org.json.JSONObject;


public interface MessagingQueue {

    void send(JSONObject message);

}
