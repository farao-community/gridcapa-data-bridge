/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.health;

import com.farao_community.farao.gridcapa.data_bridge.configuration.FtpConfiguration;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Component
@ConditionalOnProperty(prefix = "data-bridge.sources.ftp", name = "active", havingValue = "true")
public class FtpHealthIndicator implements HealthIndicator {

    private final FtpConfiguration ftpConfiguration;
    private final SessionFactory<FTPFile> ftpSessionFactory;

    public FtpHealthIndicator(SessionFactory<FTPFile> ftpSessionFactory, FtpConfiguration ftpConfiguration) {
        this.ftpSessionFactory = ftpSessionFactory;
        this.ftpConfiguration = ftpConfiguration;
    }

    @Override
    public Health health() {
        try (Session<FTPFile> session = ftpSessionFactory.getSession()) {
            if (session.test() && session.exists(ftpConfiguration.getBaseDirectory())) {
                return Health.up().build();
            }
        } catch (IOException e) {
            return Health.down().build();
        }
        return Health.down().build();
    }
}
