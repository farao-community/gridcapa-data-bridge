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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
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
    static final String GRIDCAPA_HOURLY_FREQUENCY = "hourly";
    static final String GRIDCAPA_DAILY_FREQUENCY = "daily";
    static final String GRIDCAPA_YEARLY_FREQUENCY = "yearly";

    @Value("${data-bridge.target-process}")
    private String targetProcess;
    @Value("${data-bridge.file-type}")
    private String fileType;

    @Value("${data-bridge.file-regex}")
    private String fileRegex;
    @Value("${data-bridge.file-frequency}")
    private String fileFrequency;
    @Value("${data-bridge.datetime-format}")
    private String fileDateTimePattern;

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
            if (fileFrequency.equals(GRIDCAPA_HOURLY_FREQUENCY)) {
                String timeStamp = LocalDateTime.parse(matcher.group("datetime"), DateTimeFormatter.ofPattern(fileDateTimePattern)).toString();
                return timeStamp + "/" + timeStamp;
            } else if (fileFrequency.equals(GRIDCAPA_DAILY_FREQUENCY)) {
                Date date = null;
                try {
                    date = new SimpleDateFormat(fileDateTimePattern).parse(matcher.group("datetime"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                LocalDateTime beginDateTime = date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                LocalDateTime endDateTime = beginDateTime.plusDays(1).minusHours(1);
                String beginTimeStamp = beginDateTime.toString();
                String endTimeStamp = endDateTime.toString();
                return beginTimeStamp + "/" + endTimeStamp;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

}
