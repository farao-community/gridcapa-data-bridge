/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.configuration;

import com.farao_community.farao.gridcapa.data_bridge.DataBridgeException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@ConfigurationProperties(prefix = "data-bridge.configuration")
public class DataBridgeConfiguration {

    private final String targetProcess;
    private final String zoneId;
    private final List<FileMetadataConfiguration> files;

    public DataBridgeConfiguration(String targetProcess, String zoneId, List<FileMetadataConfiguration> files) {
        this.targetProcess = targetProcess;
        this.zoneId = zoneId;
        this.files = files;
    }

    public String getTargetProcess() {
        return targetProcess;
    }

    public String getZoneId() {
        return zoneId;
    }

    public List<FileMetadataConfiguration> getFiles() {
        return files;
    }

    public FileMetadataConfiguration getFileConfigurationFromName(String fileName) {
        return files.stream()
                .filter(f -> fileName.matches(f.fileRegex()))
                .findFirst()
                .orElseThrow(() -> new DataBridgeException(String.format("Unhandled fileName: %s.", fileName)));
    }

    public FileMetadataConfiguration getFileConfigurationFromRemoteName(String remoteName) {
        return files.stream()
                .filter(f -> remoteName.matches(f.remoteFileRegex()))
                .findFirst()
                .orElseThrow(() -> new DataBridgeException(String.format("Unhandled remote fileName: %s.", remoteName)));
    }
}
