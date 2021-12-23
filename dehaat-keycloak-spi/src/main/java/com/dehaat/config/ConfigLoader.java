package com.dehaat.config;

import java.util.Properties;

public class ConfigLoader {
    private static boolean isConfigLoaded = false;
    private static final Properties configProperties = new Properties();


    public static Properties getProp() {
        if (!isConfigLoaded) {
            load();
            isConfigLoaded = true;
        }
        return configProperties;
    }

    private static void load() {
        String ENV = System.getenv(ConfigProperties.ENV.name());
        String APP_HASHCODE_DEHAAT_BUSINESS = System.getenv(ConfigProperties.APP_HASHCODE_DEHAAT_BUSINESS.name());
        String APP_HASHCODE_DEHAAT_FARMER = System.getenv(ConfigProperties.APP_HASHCODE_DEHAAT_FARMER.name());
        String MAILMAN_HOST = System.getenv(ConfigProperties.MAILMAN_HOST.name());
        String MAILMAN_SEND_TOKEN = System.getenv(ConfigProperties.MAILMAN_SEND_TOKEN.name());
        String RABBITMQ_CONNECT_URL = System.getenv(ConfigProperties.RABBITMQ_CONNECT_URL.name());
        String RABBITMQ_EXCHANGE = System.getenv(ConfigProperties.RABBITMQ_EXCHANGE.name());
        String HONEYBADGER_SETAPIKEY = System.getenv(ConfigProperties.HONEYBADGER_SETAPIKEY.name());

        if (ENV == null) {
            throw new NullPointerException("ENV is not set in environment");
        }
        if (APP_HASHCODE_DEHAAT_BUSINESS == null) {
            throw new NullPointerException("APP_HASHCODE_DEHAAT_BUSINESS is not set in environment");
        }
        if (APP_HASHCODE_DEHAAT_FARMER == null) {
            throw new NullPointerException("APP_HASHCODE_DEHAAT_FARMER is not set in environment");
        }
        if (MAILMAN_HOST == null) {
            throw new NullPointerException("MAILMAN_HOST is not set in environment");
        }
        if (MAILMAN_SEND_TOKEN == null) {
            throw new NullPointerException("MAILMAN_SEND_TOKEN is not set in environment");
        }
        if (RABBITMQ_CONNECT_URL == null) {
            throw new NullPointerException("RABBITMQ_CONNECT_URL is not set in environment");
        }
        if (RABBITMQ_EXCHANGE == null) {
            throw new NullPointerException("RABBITMQ_EXCHANGE is not set in environment");
        }
        if (HONEYBADGER_SETAPIKEY == null) {
            throw new NullPointerException("HONEYBADGER_SETAPIKEY is not set in environment");
        }

        configProperties.setProperty(ConfigProperties.ENV.name(), ENV);
        configProperties.setProperty(ConfigProperties.APP_HASHCODE_DEHAAT_BUSINESS.name(), APP_HASHCODE_DEHAAT_BUSINESS);
        configProperties.setProperty(ConfigProperties.APP_HASHCODE_DEHAAT_FARMER.name(), APP_HASHCODE_DEHAAT_FARMER);
        configProperties.setProperty(ConfigProperties.MAILMAN_HOST.name(), MAILMAN_HOST);
        configProperties.setProperty(ConfigProperties.MAILMAN_SEND_TOKEN.name(), MAILMAN_SEND_TOKEN);
        configProperties.setProperty(ConfigProperties.RABBITMQ_CONNECT_URL.name(), RABBITMQ_CONNECT_URL);
        configProperties.setProperty(ConfigProperties.RABBITMQ_EXCHANGE.name(), RABBITMQ_EXCHANGE);
        configProperties.setProperty(ConfigProperties.HONEYBADGER_SETAPIKEY.name(), HONEYBADGER_SETAPIKEY);
    }


}
