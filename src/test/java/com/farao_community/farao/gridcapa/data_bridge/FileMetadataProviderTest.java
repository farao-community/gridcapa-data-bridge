/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean
    private FileMetadataConfiguration fileMetadataConfiguration;

    private void mockConfig(String targetProcess, String fileType, String timeValidity, String fileRegex) {
        Mockito.when(fileMetadataConfiguration.getTargetProcess()).thenReturn(targetProcess);
        Mockito.when(fileMetadataConfiguration.getTimeValidity()).thenReturn(timeValidity);
        Mockito.when(fileMetadataConfiguration.getFileType()).thenReturn(fileType);
        Mockito.when(fileMetadataConfiguration.getFileRegex()).thenReturn(fileRegex);
    }

    @Test
    void checkMetadataSetCorrectlyWhenUcteFileIsCorrect() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "HOURLY",
            "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})_(?<hour>[0-9]{2})(?<minute>[0-9]{2})_.*.(uct|UCT)"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader("gridcapa_file_name", "20210101_1430_2D5_CSE1.uct")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertEquals("CSE_D2CC", metadataMap.get(FileMetadataProvider.GRIDCAPA_TARGET_PROCESS_METADATA_KEY));
        assertEquals("CGM", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_TYPE_METADATA_KEY));
        assertEquals("20210101_1430_2D5_CSE1.uct", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_NAME_KEY));
        assertEquals("2021-01-01T14:30/2021-01-01T15:30", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY));
    }

    @Test
    void checkMidnightOverpass() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "HOURLY",
            "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})_(?<hour>[0-9]{2})(?<minute>[0-9]{2})_.*.(uct|UCT)"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader("gridcapa_file_name", "20210101_2330_2D5_CSE1.uct")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertEquals("2021-01-01T23:30/2021-01-02T00:30", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY));
    }

    @Test
    void checkMetadataSetCorrectlyWithYearlyFile() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "YEARLY",
            "(?<year>[0-9]{4}).*"
        );
        Message<?> ucteFileMessage = MessageBuilder
            .withPayload("")
            .setHeader("gridcapa_file_name", "2021_test.xml")
            .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertEquals("CSE_D2CC", metadataMap.get(FileMetadataProvider.GRIDCAPA_TARGET_PROCESS_METADATA_KEY));
        assertEquals("CGM", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_TYPE_METADATA_KEY));
        assertEquals("2021_test.xml", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_NAME_KEY));
        assertEquals("2021-01-01T00:30/2022-01-01T00:30", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY));
    }

    @Test
    void checkEmptyTimeValidityIntervalWithYearlyFileAndMalformedFileName() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "YEARLY",
            "(?<year>[0-9]{4}).*"
        );
        Message<?> ucteFileMessage = MessageBuilder
            .withPayload("")
            .setHeader("gridcapa_file_name", "test_2021.xml")
            .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertEquals("CSE_D2CC", metadataMap.get(FileMetadataProvider.GRIDCAPA_TARGET_PROCESS_METADATA_KEY));
        assertEquals("CGM", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_TYPE_METADATA_KEY));
        assertEquals("test_2021.xml", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_NAME_KEY));
        assertEquals("", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY));
    }

    @Test
    void checkThrowsDataBridgeExceptionWithYearlyFileAndMalformedRegex() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "YEARLY",
            "(?<month>[0-9]{2}).*"
        );
        Message<?> ucteFileMessage = MessageBuilder
            .withPayload("")
            .setHeader("gridcapa_file_name", "09_test.xml")
            .build();
        Map<String, String> metadataMap = new HashMap<>();
        assertThrows(DataBridgeException.class, () -> fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap));
    }

    @Test
    void checkMetadataSetCorrectlyWithDailyFile() {
        mockConfig(
                "CSE_D2CC",
                "NTC_RED",
                "DAILY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*"
        );
        Message<?> fileMessage = MessageBuilder
                .withPayload("")
                .setHeader("gridcapa_file_name", "20210101_test.xml")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(fileMessage, metadataMap);

        assertEquals("CSE_D2CC", metadataMap.get(FileMetadataProvider.GRIDCAPA_TARGET_PROCESS_METADATA_KEY));
        assertEquals("NTC_RED", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_TYPE_METADATA_KEY));
        assertEquals("20210101_test.xml", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_NAME_KEY));
        assertEquals("2021-01-01T00:30/2021-01-02T00:30", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY));
    }

    @Test
    void checkThrowsDataBridgeExceptionWithDailyFileAndMalformedRegex() {
        mockConfig(
                "CSE_D2CC",
                "NTC_RED",
                "DAILY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2}).*"
        );
        Message<?> fileMessage = MessageBuilder
                .withPayload("")
                .setHeader("gridcapa_file_name", "202002_test.xml")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        assertThrows(DataBridgeException.class, () -> fileMetadataProvider.populateMetadata(fileMessage, metadataMap));
    }

    @Test
    void checkEmptyTimeValidityIntervalWithDailyFileAndMalformedFileName() {
        mockConfig(
                "CSE_D2CC",
                "NTC_RED",
                "DAILY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*"
        );
        Message<?> fileMessage = MessageBuilder
                .withPayload("")
                .setHeader("gridcapa_file_name", "test_20210203.xml")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(fileMessage, metadataMap);

        assertEquals("CSE_D2CC", metadataMap.get(FileMetadataProvider.GRIDCAPA_TARGET_PROCESS_METADATA_KEY));
        assertEquals("NTC_RED", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_TYPE_METADATA_KEY));
        assertEquals("test_20210203.xml", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_NAME_KEY));
        assertEquals("", metadataMap.get(FileMetadataProvider.GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY));
    }
}
