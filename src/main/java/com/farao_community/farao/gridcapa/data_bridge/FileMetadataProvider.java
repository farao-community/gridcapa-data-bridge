/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.gridcapa.data_bridge.configuration.DataBridgeConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.exception.DataBridgeException;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterConstants;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
public class FileMetadataProvider implements MetadataProvider {
    private static final String PREFIX_X_AMZ_META = "x-amz-meta-";
    static final String GRIDCAPA_FILE_GROUP_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_GROUP_METADATA_KEY);
    static final String GRIDCAPA_FILE_TARGET_PROCESS_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_TARGET_PROCESS_METADATA_KEY);
    static final String GRIDCAPA_FILE_TYPE_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_TYPE_METADATA_KEY);
    static final String GRIDCAPA_FILE_NAME_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY);
    static final String GRIDCAPA_DOCUMENT_ID_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_DOCUMENT_ID_METADATA_KEY);
    static final String GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY = removeXAmzMetaPrefix(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY);

    private final DataBridgeConfiguration dataBridgeConfiguration;

    public FileMetadataProvider(DataBridgeConfiguration dataBridgeConfiguration) {
        this.dataBridgeConfiguration = dataBridgeConfiguration;
    }

    private static String removeXAmzMetaPrefix(String metadataKey) {
        return metadataKey.toLowerCase().startsWith(PREFIX_X_AMZ_META) ? metadataKey.substring(PREFIX_X_AMZ_META.length()) : metadataKey;
    }

    @Override
    public void populateMetadata(Message<?> message, Map<String, String> metadata) {
        final String fileName = message.getHeaders().get(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, String.class);
        final String documentId = message.getHeaders().get(MinioAdapterConstants.DEFAULT_GRIDCAPA_DOCUMENT_ID_METADATA_KEY, String.class);
        final FileMetadataConfiguration fileMetadataConfiguration = dataBridgeConfiguration.getFileConfigurationFromName(fileName);
        metadata.put(GRIDCAPA_FILE_GROUP_METADATA_KEY, MinioAdapterConstants.DEFAULT_GRIDCAPA_INPUT_GROUP_METADATA_VALUE);
        metadata.put(GRIDCAPA_FILE_TARGET_PROCESS_METADATA_KEY, dataBridgeConfiguration.getTargetProcess());
        metadata.put(GRIDCAPA_FILE_TYPE_METADATA_KEY, fileMetadataConfiguration.fileType().name());
        metadata.put(GRIDCAPA_FILE_NAME_METADATA_KEY, fileName);
        metadata.put(GRIDCAPA_DOCUMENT_ID_METADATA_KEY, documentId);
        String fileValidityInterval = getFileValidityIntervalMetadata(fileName, fileMetadataConfiguration);
        metadata.put(GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY, fileValidityInterval);
    }

    private String getFileValidityIntervalMetadata(String fileName, FileMetadataConfiguration fileMetadataConfiguration) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        final Pattern pattern = Pattern.compile(fileMetadataConfiguration.fileRegex());
        final Matcher matcher = pattern.matcher(fileName);
        if (matcher.matches()) {
            return getDailyFileValidityIntervalMetadata(matcher);
        } else {
            return "";
        }
    }

    private String getDailyFileValidityIntervalMetadata(Matcher matcher) {
        final int year = parseOrThrow(matcher, "year");
        final int month = parseOrThrow(matcher, "month");
        final int day = parseOrThrow(matcher, "day");
        final LocalDateTime beginDateTime = LocalDateTime.of(year, month, day, 0, 30);
        final LocalDateTime endDateTime = beginDateTime.plusDays(1);
        return toUtc(beginDateTime) + "/" + toUtc(endDateTime);
    }

    private String toUtc(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.of(dataBridgeConfiguration.getZoneId())).withZoneSameInstant(ZoneOffset.UTC).toString();
    }

    private int parseOrThrow(Matcher matcher, String groupName) {
        try {
            return Integer.parseInt(matcher.group(groupName));
        } catch (IllegalArgumentException e) {
            throw new DataBridgeException(String.format("Malformed regex: %s tag is missing.", groupName), e);
        }
    }
}
