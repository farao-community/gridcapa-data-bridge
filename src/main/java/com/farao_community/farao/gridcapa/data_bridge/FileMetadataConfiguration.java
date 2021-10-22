/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Configuration
public class FileMetadataConfiguration {
    @Value("${data-bridge.target-process}")
    private String targetProcess;
    @Value("${data-bridge.file-type}")
    private String fileType;
    @Value("${data-bridge.time-validity}")
    private String timeValidity;
    @Value("${data-bridge.file-regex}")
    private String fileRegex;

    public String getTargetProcess() {
        return targetProcess;
    }

    public String getFileType() {
        return fileType;
    }

    public String getTimeValidity() {
        return timeValidity;
    }

    public String getFileRegex() {
        return fileRegex;
    }
}
