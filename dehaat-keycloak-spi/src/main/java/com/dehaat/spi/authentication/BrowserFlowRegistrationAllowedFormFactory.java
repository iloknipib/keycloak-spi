package com.dehaat.spi.authentication;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author sushil
 **/
public class BrowserFlowRegistrationAllowedFormFactory extends UsernamePasswordFormFactory {

    public static final String PROVIDER_ID = "browser-auth-registration";
    public static final BrowserFlowRegistrationAllowedForm SINGLETON = new BrowserFlowRegistrationAllowedForm();

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
        return "Mobile OTP Browser form (Registration Allowed)";
    }

    @Override
    public String getHelpText() {
        return "Validates a mobile from login form.";
    }
}

