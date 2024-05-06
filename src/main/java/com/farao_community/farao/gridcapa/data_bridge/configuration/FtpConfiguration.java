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
public class FtpConfiguration {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String baseDirectory;
    private final String fileListPersistenceFile;
    private final int dataTimeout;

    public FtpConfiguration(String host, int port, String username, String password, String baseDirectory, String fileListPersistenceFile, int dataTimeout) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.baseDirectory = baseDirectory;
        this.fileListPersistenceFile = fileListPersistenceFile;
        this.dataTimeout = dataTimeout;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public String getFileListPersistenceFile() {
        return fileListPersistenceFile;
    }

    public int getDataTimeout() {
        return dataTimeout;
    }

}
