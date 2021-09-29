package com.dehaat.spi.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserCreationEventListenerProviderFactory implements EventListenerProviderFactory {

	@Override
	public UserCreationEventListenerProvider create(KeycloakSession keycloakSession) {
		return new UserCreationEventListenerProvider(keycloakSession);
	}

	@Override
	public void init(Config.Scope scope) {
		//
	}

	@Override
	public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
		//
	}

	@Override
	public void close() {
		//
	}

	@Override
	public String getId() {
		return "pl_event_listener";
	}

}
