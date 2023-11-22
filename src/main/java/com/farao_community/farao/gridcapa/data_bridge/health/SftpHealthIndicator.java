/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.stereotype.Component;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Component
@ConditionalOnProperty(prefix = "data-bridge.sources.sftp", name = "active", havingValue = "true")
public class SftpHealthIndicator implements HealthIndicator {

    @Value("${data-bridge.sources.sftp.host}")
    private String sftpHost;
    @Value("${data-bridge.sources.sftp.port}")
    private int sftpPort;
    @Value("${data-bridge.sources.sftp.username}")
    private String sftpUsername;
    @Value("${data-bridge.sources.sftp.password}")
    private String sftpPassword;
    @Value("${data-bridge.sources.sftp.base-directory}")
    private String sftpBaseDirectory;

    @Override
    public Health health() {
        DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
        sftpSessionFactory.setHost(sftpHost);
        sftpSessionFactory.setPort(sftpPort);
        sftpSessionFactory.setUser(sftpUsername);
        sftpSessionFactory.setPassword(sftpPassword);
        sftpSessionFactory.setAllowUnknownKeys(true);
        try (SftpSession session = sftpSessionFactory.getSession()) {
            if (session.test() && session.exists(sftpBaseDirectory)) {
                return Health.up().build();
            }
        } catch (Exception e) {
            return Health.down().build();
        }
        return Health.down().build();
    }
}
