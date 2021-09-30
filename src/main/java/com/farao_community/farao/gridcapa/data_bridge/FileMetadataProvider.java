/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Component
public class FileMetadataProvider implements MetadataProvider {
    static final String GRIDCAPA_TARGET_PROCESS_METADATA_KEY = "gridcapa_process";
    static final String GRIDCAPA_FILE_TYPE_METADATA_KEY = "gridcapa_file_type";

    @Value("${data-bridge.target-process}")
    private String targetProcess;
    @Value("${data-bridge.file-type}")
    private String fileType;

    @Override
    public void populateMetadata(Message<?> message, Map<String, String> metadata) {
        metadata.put(GRIDCAPA_TARGET_PROCESS_METADATA_KEY, targetProcess);
        metadata.put(GRIDCAPA_FILE_TYPE_METADATA_KEY, fileType);
    }

}
