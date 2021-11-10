package com.dehaat.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final String KEYCLOAK_SPI_CONFIG_PATH = "KEYCLOAK_SPI_CONFIG_PATH";
    private static final String FilePath = System.getenv(KEYCLOAK_SPI_CONFIG_PATH);

    private static boolean isConfigLoaded = false;
    private static final Properties configProperties = new Properties();


    public static Properties getProp() throws IOException {
        if (!isConfigLoaded) {
            load();
            isConfigLoaded = true;
        }
        return configProperties;
    }

    private static void load() throws IOException {
        InputStream in = new FileInputStream(FilePath);
        configProperties.load(in);
        in.close();
    }


}
