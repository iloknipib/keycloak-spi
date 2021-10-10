package com.dehaat.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

/**
 * @author sushil
 */
public class DehaatJpaEntityProvider implements JpaEntityProvider {

    @Override
    public List<Class<?>> getEntities() {
        return Collections.<Class<?>>singletonList(DehaatUserMobileEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/dehaat-changelog.xml";
    }

    @Override
    public void close() {
    }

    @Override
    public String getFactoryId() {
        return DehaatJpaEntityProviderFactory.ID;
    }
}