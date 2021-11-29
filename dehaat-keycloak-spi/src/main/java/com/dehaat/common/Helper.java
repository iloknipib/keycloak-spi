package com.dehaat.common;

import com.dehaat.config.ConfigLoader;
import com.dehaat.config.ConfigProperties;
import com.dehaat.service.MessagingQueueService;
import org.jboss.logging.Logger;
import org.json.JSONObject;

import java.util.Properties;

/**
 * @author sushil
 */
public class Helper {

    private static final Logger logger = Logger.getLogger(Helper.class);
    private final static String source = "keycloak";
    private final static String entity = "user";
    private final static MessagingQueueService messagingQueueService = new MessagingQueueService();

    public static boolean isProdEnv() {
        /** Handle for non prod environment ***/
        try {
            Properties prop = ConfigLoader.getProp();
            String env = prop.getProperty(ConfigProperties.ENV.name());
            if (env != null && env.equals("PRODUCTION")) {
                return true;
            }
        } catch (Exception ex) {
            logger.error(ex);
        }
        return false;
    }

    public static JSONObject setMessageQueueData(String action, JSONObject representation) {
        MessageSchema schema = new MessageSchema();
        schema.setEntity(entity);
        schema.setSource(source);
        schema.setAction(action);
        schema.setData(representation);
        schema.setGenerated_on(System.currentTimeMillis() + "");
        JSONObject jo = new JSONObject(schema);
        return jo;
    }

    public static MessagingQueueService getMessagingQueueService() {
        return messagingQueueService;
    }


}
