/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.minio_adapter.starter.MinioAdapterConstants;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Component
public class FileMetadataProvider implements MetadataProvider {
    static final String GRIDCAPA_FILE_GROUP_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_GROUP_METADATA_KEY);
    static final String GRIDCAPA_FILE_TARGET_PROCESS_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_TARGET_PROCESS_METADATA_KEY);
    static final String GRIDCAPA_FILE_TYPE_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_TYPE_METADATA_KEY);
    static final String GRIDCAPA_FILE_NAME_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY);
    static final String GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY);

    private final FileMetadataConfiguration fileMetadataConfiguration;

    public FileMetadataProvider(FileMetadataConfiguration fileMetadataConfiguration) {
        this.fileMetadataConfiguration = fileMetadataConfiguration;
    }

    private static String removeXAmzMetaPrefix(String metadataKey) {
        String prefixToBeRemoved = "x-amz-meta-";
        return metadataKey.toLowerCase().startsWith(prefixToBeRemoved) ? metadataKey.substring(prefixToBeRemoved.length()) : metadataKey;
    }

    @Override
    public void populateMetadata(Message<?> message, Map<String, String> metadata) {
        metadata.put(GRIDCAPA_FILE_GROUP_METADATA_KEY, MinioAdapterConstants.DEFAULT_GRIDCAPA_INPUT_GROUP_METADATA_VALUE);
        metadata.put(GRIDCAPA_FILE_TARGET_PROCESS_METADATA_KEY, fileMetadataConfiguration.getTargetProcess());
        metadata.put(GRIDCAPA_FILE_TYPE_METADATA_KEY, fileMetadataConfiguration.getFileType());
        String fileName = message.getHeaders().get(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, String.class);
        metadata.put(GRIDCAPA_FILE_NAME_METADATA_KEY, fileName);
        String fileValidityInterval = getFileValidityIntervalMetadata(fileName);
        metadata.put(GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY, fileValidityInterval);
    }

    private String getFileValidityIntervalMetadata(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        Pattern pattern = Pattern.compile(fileMetadataConfiguration.getFileRegex());
        Matcher matcher = pattern.matcher(fileName);
        String timeValidity = fileMetadataConfiguration.getTimeValidity();
        if (matcher.matches()) {
            if (timeValidity.equalsIgnoreCase("hourly")) {
                return getHourlyFileValidityIntervalMetadata(matcher);
            } else if (timeValidity.equalsIgnoreCase("daily")) {
                return getDailyFileValidityIntervalMetadata(matcher);
            } else if (timeValidity.equalsIgnoreCase("yearly")) {
                return getYearlyFileValidityIntervalMetadata(matcher);
            } else {
                throw new DataBridgeException(String.format("Unhandled type of time-validity %s.", timeValidity));
            }
        } else {
            return "";
        }
    }

    private String getYearlyFileValidityIntervalMetadata(Matcher matcher) {
        int year = parseOrThrow(matcher, "year");
        int month = parseOrDefault(matcher, "month", 1);
        int dayOfMonth = parseOrDefault(matcher, "day", 1);
        LocalDateTime beginDateTime = LocalDateTime.of(year, month, dayOfMonth, 0, 30);
        LocalDateTime endDateTime = beginDateTime.plusYears(1);
        return toUtc(beginDateTime) + "/" + toUtc(endDateTime);
    }

    private String getHourlyFileValidityIntervalMetadata(Matcher matcher) {
        int year = parseOrThrow(matcher, "year");
        int month = parseOrThrow(matcher, "month");
        int day = parseOrThrow(matcher, "day");
        int hour = parseOrThrow(matcher, "hour");
        int minute = parseOrThrow(matcher, "minute");
        LocalDateTime beginDateTime = LocalDateTime.of(year, month, day, hour, minute);
        LocalDateTime endDateTime = beginDateTime.plusHours(1);
        return toUtc(beginDateTime) + "/" + toUtc(endDateTime);
    }

    private String getDailyFileValidityIntervalMetadata(Matcher matcher) {
        int year = parseOrThrow(matcher, "year");
        int month = parseOrThrow(matcher, "month");
        int day = parseOrThrow(matcher, "day");
        LocalDateTime beginDateTime = LocalDateTime.of(year, month, day, 0, 30);
        LocalDateTime endDateTime = beginDateTime.plusDays(1);
        return toUtc(beginDateTime) + "/" + toUtc(endDateTime);
    }

    private String toUtc(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.of(fileMetadataConfiguration.getZoneId())).withZoneSameInstant(ZoneOffset.UTC).toString();
    }

    private int parseOrThrow(Matcher matcher, String groupName) {
        try {
            return Integer.parseInt(matcher.group(groupName));
        } catch (IllegalArgumentException e) {
            throw new DataBridgeException(String.format("Malformed regex: %s tag is missing.", groupName), e);
        }
    }

    private int parseOrDefault(Matcher matcher, String groupName, int defaultValue) {
        try {
            return Integer.parseInt(matcher.group(groupName));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
