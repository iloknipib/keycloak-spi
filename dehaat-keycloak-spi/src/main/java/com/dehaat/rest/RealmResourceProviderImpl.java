package com.dehaat.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class RealmResourceProviderImpl implements RealmResourceProvider {
    private KeycloakSession session;

    public RealmResourceProviderImpl(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new SendOTPRestResource(session);
    }

    @Override
    public void close() {
    }
}
