package com.dehaat.service;

import com.dehaat.common.Helper;
import com.dehaat.config.ConfigLoader;
import com.dehaat.config.ConfigProperties;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.*;

/**
 * @author sushil
 **/
public class SendOTPService implements SMSSender {

    private String url;
    private String mobileNumber;
    private String otp;
    private String token;
    private int ttl;
    private String clientId;

    public SendOTPService(String mobileNumber, String otp, int ttl, String clientId) {
        this.mobileNumber = mobileNumber;
        this.otp = otp;
        this.ttl = ttl;
        this.clientId = clientId;
    }

    public boolean send() {
        boolean isProdEnv = Helper.isProdEnv();
        if (!isProdEnv) {
            // do nothing in case of dev environment
            return true;
        } else {
            boolean isOTPsent = createMailManRequest(mobileNumber, otp, ttl, clientId);
            return isOTPsent;
        }
    }

    public static boolean createMailManRequest(String mobileNumber, String otp, int ttl, String clientId) {
        Map<String, Object> payload = new HashMap<>();
        String template = "auth_otp_v1";
        String hashcode = "";
        Properties prop = null;

        try {
            prop = ConfigLoader.getProp();
        } catch (Exception ex) {

        }

        String token = prop.getProperty(ConfigProperties.MAILMAN_SEND_TOKEN.name());
        String url = prop.getProperty(ConfigProperties.MAILMAN_HOST.name());
        if (clientId.equals("farmerapp")) {
            template = "auth_otp_hashcode_farmer_v1";
            hashcode = prop.getProperty(ConfigProperties.APP_HASHCODE_DEHAAT_FARMER.name());

        }
        if (clientId.equals("businessapp")) {
            template = "auth_otp_hashcode_v1";
            hashcode = prop.getProperty(ConfigProperties.APP_HASHCODE_DEHAAT_BUSINESS.name());
        }

        payload.put("template_name", template);
        payload.put("language", "en");
        payload.put("provider", "Exotel");

        List<Object> dataList = new ArrayList<>();

        Map<String, Object> otpParams = new HashMap<>();
        otpParams.put("otp", otp);
        otpParams.put("expiry", ttl);
        otpParams.put("hashcode", hashcode);

        Map<String, Object> dataParams = new HashMap<>();
        dataParams.put("receiver", mobileNumber);
        dataParams.put("parameters", otpParams);
        dataList.add(dataParams);

        payload.put("data", dataList);


        ObjectMapper mapper = new ObjectMapper();
        try {
            //Convert Map to JSON
            String json = mapper.writeValueAsString(payload);
            System.out.println(json);
            StringEntity requestEntity = new StringEntity(json);


            HttpClient httpclient = HttpClients.createDefault();
            HttpPost postMethod = new HttpPost(url);
            postMethod.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + token);
            postMethod.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            postMethod.setEntity(requestEntity);

            HttpResponse response = httpclient.execute(postMethod);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return true;
            }
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
