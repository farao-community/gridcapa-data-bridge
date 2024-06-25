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
@ConfigurationProperties(prefix = "data-bridge.sources.ftp")
@ConditionalOnProperty(prefix = "data-bridge.sources.ftp", name = "active", havingValue = "true")
public class FtpConfiguration extends SourceConfiguration {

    private final FtpTimeouts timeouts;

    public FtpConfiguration(String host, int port, String username, String password, String baseDirectory, int pollingDelayInMs, int maxMessagesPerPoll, int maxPoolSize, FtpTimeouts timeouts) {
        super(host, port, username, password, baseDirectory, pollingDelayInMs, maxMessagesPerPoll, maxPoolSize);
        this.timeouts = timeouts;
    }

    public FtpTimeouts getFtpTimeouts() {
        return  timeouts;
    }
}
