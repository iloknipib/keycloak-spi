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
        configProperties.setProperty(ConfigProperties.ENV.name(), System.getenv(ConfigProperties.ENV.name()));
        configProperties.setProperty(ConfigProperties.APP_HASHCODE_DEHAAT_BUSINESS.name(), System.getenv(ConfigProperties.APP_HASHCODE_DEHAAT_BUSINESS.name()));
        configProperties.setProperty(ConfigProperties.APP_HASHCODE_DEHAAT_FARMER.name(), System.getenv(ConfigProperties.APP_HASHCODE_DEHAAT_FARMER.name()));
        configProperties.setProperty(ConfigProperties.MAILMAN_HOST.name(), System.getenv(ConfigProperties.MAILMAN_HOST.name()));
        configProperties.setProperty(ConfigProperties.MAILMAN_SEND_TOKEN.name(), System.getenv(ConfigProperties.MAILMAN_SEND_TOKEN.name()));
    }


}