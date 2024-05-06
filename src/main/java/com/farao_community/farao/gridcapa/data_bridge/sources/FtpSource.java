
/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;
import com.farao_community.farao.gridcapa.data_bridge.DataBridgeException;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FtpConfiguration;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel  at rte-france.com>}
 */
@Configuration
@ConditionalOnProperty(prefix = "data-bridge.sources.ftp", name = "active", havingValue = "true")
public class FtpSource {

    @Bean
    public DefaultFtpSessionFactory ftpSessionFactory(ConfigurableEnvironment environment) {
        FtpConfiguration ftpConfiguration = Binder.get(environment)
                .bind("data-bridge.sources.ftp", Bindable.of(FtpConfiguration.class))
                .orElseThrow(() -> new DataBridgeException("Unable to create ftpSessionFactory: missing ftp config"));
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setHost(ftpConfiguration.getHost());
        ftpSessionFactory.setPort(ftpConfiguration.getPort());
        ftpSessionFactory.setUsername(ftpConfiguration.getUsername());
        ftpSessionFactory.setPassword(ftpConfiguration.getPassword());
        ftpSessionFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        ftpSessionFactory.setDataTimeout(ftpConfiguration.getDataTimeout());
        return ftpSessionFactory;
    }

    @Bean
    public FtpDynamicBeanCreator createFtpBeans(SessionFactory<FTPFile> ftpSessionFactory, ConfigurableEnvironment environment) {
        return new FtpDynamicBeanCreator(ftpSessionFactory, environment);
    }
}
