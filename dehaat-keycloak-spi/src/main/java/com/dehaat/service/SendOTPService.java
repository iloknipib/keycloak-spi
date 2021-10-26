package com.dehaat.service;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sushil
 **/
public class SendOTPService implements SMSSender {

    private String url;
    private String mobileNumber;
    private String otp;
    private String token;
    private int ttl;

    public SendOTPService(String url, String mobileNumber, String otp, int ttl, String token) {
        this.url = url;
        this.mobileNumber = mobileNumber;
        this.otp = otp;
        this.ttl = ttl;
        this.token = token;
    }

    public boolean send() {
        boolean isOTPsent = createMailManRequest(url, mobileNumber, otp, ttl, token);
        return isOTPsent;
    }

    public static boolean createMailManRequest(String url, String mobileNumber, String otp, int ttl, String token) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("language", "en");
        payload.put("template_name", "auth_otp_hashcode_farmer_v1");
        payload.put("provider", "Exotel");

        List<Object> dataList = new ArrayList<>();

        Map<String, Object> otpParams = new HashMap<>();
        otpParams.put("otp", otp);
        otpParams.put("expiry", ttl);
        otpParams.put("hashcode", "");

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
//            postMethod.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + "M2M0NWU2YTEtNGNkYi00ZDkzLTgxNWUtMWI0ZjRiNzI0NTgzOjM0NzA4MDVkLTYzMDAtNDQxNC05N2QzLThkNjZiNDJkYmE3YQ==");
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
