/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.minio_adapter.starter.MinioAdapterConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@SpringBootTest
class FileMetadataProviderTest {

    @Autowired
    private FileMetadataProvider fileMetadataProvider;

    private void mockConfig(MessageBuilder messageBuilder, String targetProcess, String fileType, String timeValidity, String fileRegex, String zoneId) {

        messageBuilder.setHeader("target-process", targetProcess)
                .setHeader("file-validity", timeValidity)
                .setHeader("file-type", fileType)
                .setHeader("file-pattern", fileRegex)
                .setHeader("zone", zoneId);
    }

    @Test
    void checkMetadataSetCorrectlyWhenUcteFileIsCorrect() {

        MessageBuilder messageBuilder = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210101_1430_2D5_CSE1.uct");
        mockConfig(
                messageBuilder,
                "CSE_D2CC",
                "CGM",
                "HOURLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})_(?<hour>[0-9]{2})(?<minute>[0-9]{2})_.*.(uct|UCT)",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "20210101_1430_2D5_CSE1.uct", "2021-01-01T13:30Z/2021-01-01T14:30Z");
    }

    @Test
    void checkMidnightOverpass() {

        MessageBuilder messageBuilder = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210101_2330_2D5_CSE1.uct");
        mockConfig(
                messageBuilder, "CSE_D2CC",
                "CGM",
                "HOURLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})_(?<hour>[0-9]{2})(?<minute>[0-9]{2})_.*.(uct|UCT)",
                "UCT"
        );
        Message<?> ucteFileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("file-validity", "hourly");
        metadataMap.put("zone", "Europe/Paris");
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "20210101_2330_2D5_CSE1.uct", "2021-01-01T23:30Z/2021-01-02T00:30Z");
    }

    @Test
    void checkMetadataSetCorrectlyWithYearlyFile() {

        MessageBuilder messageBuilder = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "2021_test.xml");
        mockConfig(
                messageBuilder, "CSE_D2CC",
                "CGM",
                "YEARLY",
                "(?<year>[0-9]{4}).*",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("file-validity", "yearly");
        metadataMap.put("zone", "Europe/Paris");
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "2021_test.xml", "2020-12-31T23:30Z/2021-12-31T23:30Z");
    }

    @Test
    void checkMetadataSetCorrectlyWithYearlyFileStartingOnADayInSummer() {

        MessageBuilder messageBuilder = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210615_test.xml");
        mockConfig(
                messageBuilder, "CSE_D2CC",
                "CGM",
                "YEARLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("file-validity", "yearly");
        metadataMap.put("zone", "Europe/Paris");
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "20210615_test.xml", "2021-06-14T22:30Z/2022-06-14T22:30Z");
    }

    @Test
    void checkMetadataSetCorrectlyWithYearlyFileStartingOnADayDuringWinter() {

        MessageBuilder messageBuilder = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20211105_test.xml");
        mockConfig(
                messageBuilder, "CSE_D2CC",
                "CGM",
                "YEARLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        metadataMap.put("file-validity", "yearly");
        metadataMap.put("zone", "Europe/Paris");
        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "20211105_test.xml", "2021-11-04T23:30Z/2022-11-04T23:30Z");
    }

    @Test
    void checkEmptyTimeValidityIntervalWithYearlyFileAndMalformedFileName() {

        MessageBuilder messageBuilder = MessageBuilder
             .withPayload("")
             .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "test_2021.xml");
        mockConfig(
                messageBuilder, "CSE_D2CC",
                "CGM",
                "YEARLY",
                "(?<year>[0-9]{4}).*",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("file-validity", "yearly");
        metadataMap.put("zone", "Europe/Paris");
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "test_2021.xml", "");
    }

    @Test
    void checkThrowsDataBridgeExceptionWithYearlyFileAndMalformedRegex() {

        MessageBuilder messageBuilder = MessageBuilder
             .withPayload("")
             .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "09_test.xml");
        mockConfig(
                messageBuilder, "CSE_D2CC",
                "CGM",
                "YEARLY",
                "(?<month>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        assertThrows(DataBridgeException.class, () -> fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap));
    }

    @Test
    void checkMetadataSetCorrectlyWithDailyFile() {

        MessageBuilder messageBuilder = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210101_test.xml");
        mockConfig(
                messageBuilder, "CSE_D2CC",
                "NTC_RED",
                "DAILY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> fileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("file-validity", "daily");
        metadataMap.put("zone", "Europe/Paris");
        fileMetadataProvider.populateMetadata(fileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "NTC_RED", "20210101_test.xml", "2020-12-31T23:30Z/2021-01-01T23:30Z");
    }

    @Test
    void checkThrowsDataBridgeExceptionWithDailyFileAndMalformedRegex() {

        MessageBuilder messageBuilder = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "202002_test.xml");
        mockConfig(
                messageBuilder, "CSE_D2CC",
                "NTC_RED",
                "DAILY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> fileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        assertThrows(DataBridgeException.class, () -> fileMetadataProvider.populateMetadata(fileMessage, metadataMap));
    }

    @Test
    void checkEmptyTimeValidityIntervalWithDailyFileAndMalformedFileName() {

        MessageBuilder messageBuilder = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "test_20210203.xml");
        mockConfig(
                messageBuilder, "CSE_D2CC",
                "NTC_RED",
                "DAILY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> fileMessage = messageBuilder.build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(fileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "NTC_RED", "test_20210203.xml", "");
    }

    void assertAllInputFileMetadataEquals(Map<String, String> actualMetadata, String targetProcess, String fileType, String fileName, String fileValidityInterval) {
        assertEquals(MinioAdapterConstants.DEFAULT_GRIDCAPA_INPUT_GROUP_METADATA_VALUE, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_GROUP_METADATA_KEY));
        assertEquals(targetProcess, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_TARGET_PROCESS_METADATA_KEY));
        assertEquals(fileType, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_TYPE_METADATA_KEY));
        assertEquals(fileName, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_NAME_METADATA_KEY));
        assertEquals(fileValidityInterval, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY));
    }
}
