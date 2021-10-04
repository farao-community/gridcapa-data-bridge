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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Component
public class FileMetadataProvider implements MetadataProvider {
    public static final String GRIDCAPA_FILE_NAME_KEY = "gridcapa_file_name";

    static final String GRIDCAPA_TARGET_PROCESS_METADATA_KEY = "gridcapa_process";
    static final String GRIDCAPA_FILE_TYPE_METADATA_KEY = "gridcapa_file_type";
    static final String GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY = "gridcapa_file_validity_interval";

    @Value("${data-bridge.target-process}")
    private String targetProcess;
    @Value("${data-bridge.file-type}")
    private String fileType;

    @Value("${data-bridge.file-regex}")
    private String fileRegex;

    @Override
    public void populateMetadata(Message<?> message, Map<String, String> metadata) {
        metadata.put(GRIDCAPA_TARGET_PROCESS_METADATA_KEY, targetProcess);
        metadata.put(GRIDCAPA_FILE_TYPE_METADATA_KEY, fileType);
        String fileValidityInterval = getFileValidityIntervalMetadata(message);
        metadata.put(GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY, fileValidityInterval);
    }

    private String getFileValidityIntervalMetadata(Message<?> message) {
        String filename = message.getHeaders().get(GRIDCAPA_FILE_NAME_KEY, String.class);
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        Pattern pattern = Pattern.compile(fileRegex);
        Matcher matcher = pattern.matcher(filename);
        if (matcher.matches()) {
            int year = Integer.parseInt(matcher.group("year"));
            int month = Integer.parseInt(matcher.group("month"));
            int day = Integer.parseInt(matcher.group("day"));
            int hour = Integer.parseInt(matcher.group("hour"));
            int minute = Integer.parseInt(matcher.group("minute"));
            String timeStamp = LocalDateTime.of(year, month, day, hour, minute).toString();
            return timeStamp + "/" + timeStamp;
        } else {
            return "";
        }
    }

}
