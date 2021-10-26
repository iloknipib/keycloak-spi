package com.dehaat.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.models.jpa.entities.UserAttributeEntity;

import java.util.Collections;
import java.util.List;

/**
 * @author sushil
 */
public class JpaEntityProviderImpl implements JpaEntityProvider {

    @Override
    public List<Class<?>> getEntities() {
        return Collections.<Class<?>>singletonList(UserAttributeEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/custom-changelog.xml";
    }

    @Override
    public void close() {
    }

    @Override
    public String getFactoryId() {
        return JpaEntityProviderFactoryImpl.ID;
    }
}