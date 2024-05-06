/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;

import com.farao_community.farao.gridcapa.data_bridge.DataBridgeException;
import com.farao_community.farao.gridcapa.data_bridge.configuration.SftpConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel  at rte-france.com>}
 */
@Configuration
@ConditionalOnProperty(prefix = "data-bridge.sources.sftp", name = "active", havingValue = "true")
public class SftpSource {

    @Bean
    public DefaultSftpSessionFactory sftpSessionFactory(ConfigurableEnvironment environment) {
        SftpConfiguration sftpConfiguration = Binder.get(environment)
                .bind("data-bridge.sources.sftp", Bindable.of(SftpConfiguration.class))
                .orElseThrow(() -> new DataBridgeException("Unable to create sftpSessionFactory: missing sftp config"));
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(sftpConfiguration.getHost());
        factory.setPort(sftpConfiguration.getPort());
        factory.setUser(sftpConfiguration.getUsername());
        factory.setPassword(sftpConfiguration.getPassword());
        factory.setAllowUnknownKeys(true);
        return factory;
    }

    @Bean
    public SftpDynamicBeanCreator createSftpBeans(DefaultSftpSessionFactory sftpSessionFactory, ConfigurableEnvironment environment) {
        return new SftpDynamicBeanCreator(sftpSessionFactory, environment);
    }
}
