/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel  at rte-france.com>}
 */
@ConfigurationProperties(prefix = "data-bridge.sources.sftp")
@ConditionalOnProperty(prefix = "data-bridge.sources.sftp", name = "active", havingValue = "true")
public class SftpConfiguration extends SourceConfiguration {

    public SftpConfiguration(String host, int port, String username, String password, String baseDirectory, int pollingDelayInMs, String fileListPersistenceFile, int maxMessagesPerPoll) {
        super(host, port, username, password, baseDirectory, pollingDelayInMs, fileListPersistenceFile, maxMessagesPerPoll);
    }
}
