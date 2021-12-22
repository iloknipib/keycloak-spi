package com.dehaat.common;

import com.dehaat.config.ConfigLoader;
import com.dehaat.config.ConfigProperties;
import io.honeybadger.reporter.HoneybadgerReporter;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.config.StandardConfigContext;

public class HoneybadgerErrorReporter {

    private static NoticeReporter reporter;
    private static String Api_key;
    private static String Environment;

    public HoneybadgerErrorReporter() {
    }

    public static NoticeReporter getReporter() {
        if (reporter != null) {
            return reporter;
        }
        Api_key = ConfigLoader.getProp().getProperty(ConfigProperties.HONEYBADGER_SETAPIKEY.name());
        Environment = ConfigLoader.getProp().getProperty(ConfigProperties.ENV.name());
        StandardConfigContext config = new StandardConfigContext();
        config.setApiKey(Api_key)
                .setEnvironment(Environment);
        ;
        reporter = new HoneybadgerReporter(config);
        return reporter;
    }
}
