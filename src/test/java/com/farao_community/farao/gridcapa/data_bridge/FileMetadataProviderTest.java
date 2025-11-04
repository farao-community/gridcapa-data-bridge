/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.gridcapa.data_bridge.configuration.DataBridgeConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.List;
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

    @MockitoBean
    private DataBridgeConfiguration dataBridgeConfiguration;
    @MockitoBean
    private FileMetadataConfiguration fileMetadataConfiguration;

    private void mockConfig(String targetProcess, String fileType, String timeValidity, String fileRegex, String zoneId) {
        Mockito.when(dataBridgeConfiguration.getFiles()).thenReturn(List.of(fileMetadataConfiguration));
        Mockito.when(dataBridgeConfiguration.getTargetProcess()).thenReturn(targetProcess);
        Mockito.when(fileMetadataConfiguration.timeValidity()).thenReturn(timeValidity);
        Mockito.when(fileMetadataConfiguration.fileType()).thenReturn(fileType);
        Mockito.when(fileMetadataConfiguration.fileRegex()).thenReturn(fileRegex);
        Mockito.when(dataBridgeConfiguration.getZoneId()).thenReturn(zoneId);
        Mockito.when(dataBridgeConfiguration.getFileConfigurationFromName(Mockito.anyString())).thenReturn(fileMetadataConfiguration);
    }

    @Test
    void checkMetadataSetCorrectlyWhenUcteFileIsCorrect() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "HOURLY",
            "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})_(?<hour>[0-9]{2})(?<minute>[0-9]{2})_.*.(uct|UCT)",
            "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210101_1430_2D5_CSE1.uct")
            .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "20210101_1430_2D5_CSE1.uct", "2021-01-01T13:30Z/2021-01-01T14:30Z");
    }

    @Test
    void checkMidnightOverpass() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "HOURLY",
            "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})_(?<hour>[0-9]{2})(?<minute>[0-9]{2})_.*.(uct|UCT)",
            "UCT"
        );
        Message<?> ucteFileMessage = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210101_2330_2D5_CSE1.uct")
            .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "20210101_2330_2D5_CSE1.uct", "2021-01-01T23:30Z/2021-01-02T00:30Z");
    }

    @Test
    void checkMetadataSetCorrectlyWithYearlyFile() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "YEARLY",
            "(?<year>[0-9]{4}).*",
            "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "2021_test.xml")
            .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "2021_test.xml", "2020-12-31T23:30Z/2021-12-31T23:30Z");
    }

    @Test
    void checkMetadataSetCorrectlyWithYearlyFileStartingOnADayInSummer() {
        mockConfig(
                "CSE_D2CC",
                "CGM",
                "YEARLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210615_test.xml")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "20210615_test.xml", "2021-06-14T22:30Z/2022-06-14T22:30Z");
    }

    @Test
    void checkMetadataSetCorrectlyWithYearlyFileStartingOnADayDuringWinter() {
        mockConfig(
                "CSE_D2CC",
                "CGM",
                "YEARLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20211105_test.xml")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "20211105_test.xml", "2021-11-04T23:30Z/2022-11-04T23:30Z");
    }

    @Test
    void checkEmptyTimeValidityIntervalWithYearlyFileAndMalformedFileName() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "YEARLY",
            "(?<year>[0-9]{4}).*",
            "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "test_2021.xml")
            .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "CGM", "test_2021.xml", "");
    }

    @Test
    void checkThrowsDataBridgeExceptionWithYearlyFileAndMalformedRegex() {
        mockConfig(
            "CSE_D2CC",
            "CGM",
            "YEARLY",
            "(?<month>[0-9]{2}).*",
            "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "09_test.xml")
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
            "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
            "Europe/Paris"
        );
        Message<?> fileMessage = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210101_test.xml")
            .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(fileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "NTC_RED", "20210101_test.xml", "2020-12-31T23:30Z/2021-01-01T23:30Z");
    }

    @Test
    void checkThrowsDataBridgeExceptionWithDailyFileAndMalformedRegex() {
        mockConfig(
            "CSE_D2CC",
            "NTC_RED",
            "DAILY",
            "(?<year>[0-9]{4})(?<month>[0-9]{2}).*",
            "Europe/Paris"
        );
        Message<?> fileMessage = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "202002_test.xml")
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
            "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
            "Europe/Paris"
        );
        Message<?> fileMessage = MessageBuilder
            .withPayload("")
            .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "test_20210203.xml")
            .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(fileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap, "CSE_D2CC", "NTC_RED", "test_20210203.xml", "");
    }

    @Test
    void checkSpecificZoneIdUsed() {
        mockConfig(
                "CORE-VALID-IDCC",
                "VERTICES",
                "HOURLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-3]{1}[0-9]{1})-(?<hour>[0-2]{1}[0-9]{1})(?<minute>00[abAB]{0,1})-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v(?<version>[0-9]*).(csv|CSV)",
                "UCT"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20250101-2000-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertAllInputFileMetadataEquals(metadataMap, "CORE-VALID-IDCC", "VERTICES", "20250101-2000-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv", "2025-01-01T20:00Z/2025-01-01T21:00Z");
    }

    @Test
    void checkFileSpecificZoneIdUsedDstBeforeWinterTime() {
        mockConfig(
                "CORE-VALID-IDCC",
                "VERTICES",
                "HOURLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-3]{1}[0-9]{1})-(?<hour>[0-2]{1}[0-9]{1})(?<minute>00[abAB]{0,1})-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v(?<version>[0-9]*).(csv|CSV)",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20251026-0200A-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertAllInputFileMetadataEquals(metadataMap, "CORE-VALID-IDCC", "VERTICES", "20251026-0200A-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv", "2025-10-26T00:00Z/2025-10-26T01:00Z");
    }

    @Test
    void checkFileSpecificZoneIdUsedDstAfterWinterTime() {
        mockConfig(
                "CORE-VALID-IDCC",
                "VERTICES",
                "HOURLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-3]{1}[0-9]{1})-(?<hour>[0-2]{1}[0-9]{1})(?<minute>00[abAB]{0,1})-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v(?<version>[0-9]*).(csv|CSV)",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20251026-0200B-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertAllInputFileMetadataEquals(metadataMap, "CORE-VALID-IDCC", "VERTICES", "20251026-0200B-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv", "2025-10-26T01:00Z/2025-10-26T02:00Z");
    }

    @Test
    void checkFileSpecificZoneIdUsedDstBeforeSummerTime() {
        mockConfig(
                "CORE-VALID-IDCC",
                "VERTICES",
                "HOURLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-3]{1}[0-9]{1})-(?<hour>[0-2]{1}[0-9]{1})(?<minute>00[abAB]{0,1})-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v(?<version>[0-9]*).(csv|CSV)",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20250330-0300-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertAllInputFileMetadataEquals(metadataMap, "CORE-VALID-IDCC", "VERTICES", "20250330-0300-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv", "2025-03-30T01:00Z/2025-03-30T02:00Z");
    }

    @Test
    void checkFileSpecificZoneIdUsedDstAfterSummerTime() {
        mockConfig(
                "CORE-VALID-IDCC",
                "VERTICES",
                "HOURLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-3]{1}[0-9]{1})-(?<hour>[0-2]{1}[0-9]{1})(?<minute>00[abAB]{0,1})-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v(?<version>[0-9]*).(csv|CSV)",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20250330-0400-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertAllInputFileMetadataEquals(metadataMap, "CORE-VALID-IDCC", "VERTICES", "20250330-0400-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv", "2025-03-30T02:00Z/2025-03-30T03:00Z");
    }

    @Test
    void checkEmptyFileNameGivesNoInterval() {
        mockConfig(
                "CORE-VALID-IDCC",
                "VERTICES",
                "HOURLY",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-3]{1}[0-9]{1})-(?<hour>[0-2]{1}[0-9]{1})(?<minute>00[abAB]{0,1})-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v(?<version>[0-9]*).(csv|CSV)",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertAllInputFileMetadataEquals(metadataMap, "CORE-VALID-IDCC", "VERTICES", "", "");
    }

    @Test
    void checkBadTimeValidityThrows() {
        mockConfig(
                "CORE-VALID-IDCC",
                "VERTICES",
                "DECINAL",
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-3]{1}[0-9]{1})-(?<hour>[0-2]{1}[0-9]{1})(?<minute>00[abAB]{0,1})-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v(?<version>[0-9]*).(csv|CSV)",
                "Europe/Paris"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20250330-0400-FID2-701-INIT_VIRG_REFBAL_PRES_REPREVERTICES-v1.csv")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        assertThrows(DataBridgeException.class, () -> fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap));
    }

    void assertAllInputFileMetadataEquals(Map<String, String> actualMetadata, String targetProcess, String fileType, String fileName, String fileValidityInterval) {
        assertEquals(MinioAdapterConstants.DEFAULT_GRIDCAPA_INPUT_GROUP_METADATA_VALUE, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_GROUP_METADATA_KEY));
        assertEquals(targetProcess, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_TARGET_PROCESS_METADATA_KEY));
        assertEquals(fileType, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_TYPE_METADATA_KEY));
        assertEquals(fileName, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_NAME_METADATA_KEY));
        assertEquals(fileValidityInterval, actualMetadata.get(FileMetadataProvider.GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY));
    }
}
