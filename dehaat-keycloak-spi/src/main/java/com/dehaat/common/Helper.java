package com.dehaat.common;

import com.dehaat.config.ConfigLoader;
import com.dehaat.config.ConfigProperties;
import org.jboss.logging.Logger;

import java.util.Properties;

/**
 * @author sushil
 */
public class Helper {

    private static final Logger logger = Logger.getLogger(Helper.class);

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


}
