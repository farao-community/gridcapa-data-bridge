/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.health;

import com.farao_community.farao.gridcapa.data_bridge.configuration.SftpConfiguration;
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

    private final DefaultSftpSessionFactory sftpSessionFactory;
    private final SftpConfiguration sftpConfiguration;

    public SftpHealthIndicator(DefaultSftpSessionFactory sftpSessionFactory, SftpConfiguration sftpConfiguration) {
        this.sftpSessionFactory = sftpSessionFactory;
        this.sftpConfiguration = sftpConfiguration;
    }
    @Override
    public Health health() {
        sftpSessionFactory.setAllowUnknownKeys(true);
        try (SftpSession session = sftpSessionFactory.getSession()) {
            if (session.test() && session.exists(sftpConfiguration.getBaseDirectory())) {
                return Health.up().build();
            }
        } catch (Exception e) {
            return Health.down().build();
        }
        return Health.down().build();
    }
}
