package com.dehaat.spi.authentication;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author sushil
 **/
public class BrowserFlowFormFactory extends UsernamePasswordFormFactory {

    public static final String PROVIDER_ID = "browser-authenticator";
    public static final BrowserFlowForm SINGLETON = new BrowserFlowForm();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Mobile OTP Browser form";
    }

    @Override
    public String getHelpText() {
        return "Validates a mobile from login form.";
    }
}

