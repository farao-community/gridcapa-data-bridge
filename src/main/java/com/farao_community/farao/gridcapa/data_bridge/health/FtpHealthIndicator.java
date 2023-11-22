/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.health;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Component
@ConditionalOnProperty(prefix = "data-bridge.sources.ftp", name = "active", havingValue = "true")
public class FtpHealthIndicator implements HealthIndicator {

    @Value("${data-bridge.sources.ftp.host}")
    private String ftpHost;
    @Value("${data-bridge.sources.ftp.port}")
    private int ftpPort;
    @Value("${data-bridge.sources.ftp.username}")
    private String ftpUsername;
    @Value("${data-bridge.sources.ftp.password}")
    private String ftpPassword;
    @Value("${data-bridge.sources.ftp.base-directory}")
    private String ftpBaseDirectory;
    @Value("${data-bridge.sources.ftp.data-timeout:60000}")
    private int ftpDataTimeout;

    @Override
    public Health health() {
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setHost(ftpHost);
        ftpSessionFactory.setPort(ftpPort);
        ftpSessionFactory.setUsername(ftpUsername);
        ftpSessionFactory.setPassword(ftpPassword);
        ftpSessionFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        ftpSessionFactory.setDataTimeout(ftpDataTimeout);
        try (FtpSession session = ftpSessionFactory.getSession()) {
            if (session.test() && session.exists(ftpBaseDirectory)) {
                return Health.up().build();
            }
        } catch (IOException e) {
            return Health.down().build();
        }
        return Health.down().build();
    }
}
