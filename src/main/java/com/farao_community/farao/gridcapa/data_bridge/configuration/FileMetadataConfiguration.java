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
public final class FileMetadataConfiguration {
    private final String fileType;
    private final String fileRegex;
    private final String timeValidity;
    private final String remoteFileRegex;
    private final boolean doUnzip;
    private final String sourceDirectory;
    private final String sinkDirectory;
    private final String fileListPersistenceFile;

    public FileMetadataConfiguration(String fileType, String fileRegex, String timeValidity, String remoteFileRegex,
                                     Boolean doUnzip, String sourceDirectory, String sinkDirectory, String fileListPersistenceFile) {
        this.fileType = fileType;
        this.fileRegex = fileRegex;
        this.timeValidity = timeValidity;
        this.remoteFileRegex = remoteFileRegex;
        this.doUnzip = doUnzip != null ? doUnzip : true;
        this.sourceDirectory = sourceDirectory;
        this.sinkDirectory = sinkDirectory;
        this.fileListPersistenceFile = fileListPersistenceFile;
    }

    public String fileType() {
        return fileType;
    }

    public String fileRegex() {
        return fileRegex;
    }

    public String timeValidity() {
        return timeValidity;
    }

    public String remoteFileRegex() {
        return remoteFileRegex;
    }

    public boolean doUnzip() {
        return doUnzip;
    }

    public String sourceDirectory() {
        return sourceDirectory;
    }

    public String sinkDirectory() {
        return sinkDirectory;
    }

    public String getFileListPersistenceFile() {
        return fileListPersistenceFile;
    }
}
