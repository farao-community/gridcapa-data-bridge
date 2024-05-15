/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.configuration;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel  at rte-france.com>}
 */
public class SourceConfiguration {

    protected final String host;
    protected final int port;
    protected final String username;
    protected final String password;
    protected final String baseDirectory;
    protected final int pollingDelayInMs;
    protected final String fileListPersistenceFile;
    protected final int maxMessagesPerPoll;
    protected final int maxPoolSize;

    public SourceConfiguration(String host, int port, String username, String password, String baseDirectory, int pollingDelayInMs, String fileListPersistenceFile, int maxMessagesPerPoll, int maxPoolSize) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.baseDirectory = baseDirectory;
        this.pollingDelayInMs = pollingDelayInMs;
        this.fileListPersistenceFile = fileListPersistenceFile;
        this.maxMessagesPerPoll = maxMessagesPerPoll;
        this.maxPoolSize = maxPoolSize;
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

    public int getPollingDelayInMs() {
        return pollingDelayInMs;
    }

    public String getFileListPersistenceFile() {
        return fileListPersistenceFile;
    }

    public int getMaxMessagesPerPoll() {
        return maxMessagesPerPoll;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }
}
