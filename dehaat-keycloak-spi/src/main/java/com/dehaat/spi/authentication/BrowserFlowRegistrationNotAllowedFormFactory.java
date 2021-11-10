package com.dehaat.spi.authentication;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author sushil
 **/
public class BrowserFlowRegistrationNotAllowedFormFactory extends UsernamePasswordFormFactory {

    public static final String PROVIDER_ID = "browser-auth-no-registration";
    public static final BrowserFlowRegistrationNotAllowedForm SINGLETON = new BrowserFlowRegistrationNotAllowedForm();

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
        return "Mobile OTP Browser form (No Registration)";
    }

    @Override
    public String getHelpText() {
        return "Validates a mobile from login form.";
    }
}

