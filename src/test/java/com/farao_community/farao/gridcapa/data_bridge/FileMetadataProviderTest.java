/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.gridcapa.data_bridge.configuration.DataBridgeConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileType;
import com.farao_community.farao.gridcapa.data_bridge.exception.DataBridgeException;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterConstants;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class FileMetadataProviderTest {

    @Autowired
    private FileMetadataProvider fileMetadataProvider;

    @MockBean
    private DataBridgeConfiguration dataBridgeConfiguration;
    @MockBean
    private FileMetadataConfiguration fileMetadataConfiguration;

    private void mockConfig(String targetProcess, FileType fileType, String fileRegex, String zoneId) {
        Mockito.when(dataBridgeConfiguration.getFiles()).thenReturn(List.of(fileMetadataConfiguration));
        Mockito.when(dataBridgeConfiguration.getTargetProcess()).thenReturn(targetProcess);
        Mockito.when(fileMetadataConfiguration.fileType()).thenReturn(fileType);
        Mockito.when(fileMetadataConfiguration.fileRegex()).thenReturn(fileRegex);
        Mockito.when(dataBridgeConfiguration.getZoneId()).thenReturn(zoneId);
        Mockito.when(dataBridgeConfiguration.getFileConfigurationFromName(Mockito.anyString())).thenReturn(fileMetadataConfiguration);
    }

    @Test
    void checkMidnightOverpass() {
        mockConfig(
                "CSE_D2CC",
                FileType.CGM,
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})_(?<hour>[0-9]{2})(?<minute>[0-9]{2})_.*.(uct|UCT)",
                "UCT"
        );
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210101_2330_2D5_CSE1.uct")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_DOCUMENT_ID_METADATA_KEY, "documentIdMidnight")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);
        assertAllInputFileMetadataEquals(metadataMap,
                "CSE_D2CC",
                "CGM",
                "20210101_2330_2D5_CSE1.uct",
                "2021-01-01T00:30Z/2021-01-02T00:30Z",
                "documentIdMidnight");
    }

    @Test
    void checkMetadataSetCorrectly() {
        mockConfig(
                "CSE_D2CC",
                FileType.CBCORA,
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> fileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "20210101_test.xml")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_DOCUMENT_ID_METADATA_KEY, "documentIdDaily")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(fileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap,
                "CSE_D2CC",
                "CBCORA",
                "20210101_test.xml",
                "2020-12-31T23:30Z/2021-01-01T23:30Z",
                "documentIdDaily");
    }

    @Test
    void checkThrowsDataBridgeExceptionWithMalformedRegex() {
        mockConfig(
                "CSE_D2CC",
                FileType.CBCORA,
                "(?<year>[0-9]{4})(?<month>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> fileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "202002_test.xml")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        Assertions.assertThatExceptionOfType(DataBridgeException.class)
                .isThrownBy(() -> fileMetadataProvider.populateMetadata(fileMessage, metadataMap));
    }

    @Test
    void checkEmptyTimeValidityIntervalWithMalformedFileName() {
        mockConfig(
                "CSE_D2CC",
                FileType.CBCORA,
                "(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2}).*",
                "Europe/Paris"
        );
        Message<?> fileMessage = MessageBuilder
                .withPayload("")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, "test_20210203.xml")
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_DOCUMENT_ID_METADATA_KEY, "documentIdDailyMalformed")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(fileMessage, metadataMap);

        assertAllInputFileMetadataEquals(metadataMap,
                "CSE_D2CC",
                "CBCORA",
                "test_20210203.xml",
                "",
                "documentIdDailyMalformed");
    }

    void assertAllInputFileMetadataEquals(Map<String, String> actualMetadata, String targetProcess, String fileType, String fileName, String fileValidityInterval, String documentId) {
        Assertions.assertThat(actualMetadata)
                .containsEntry(FileMetadataProvider.GRIDCAPA_FILE_GROUP_METADATA_KEY, MinioAdapterConstants.DEFAULT_GRIDCAPA_INPUT_GROUP_METADATA_VALUE)
                .containsEntry(FileMetadataProvider.GRIDCAPA_FILE_TARGET_PROCESS_METADATA_KEY, targetProcess)
                .containsEntry(FileMetadataProvider.GRIDCAPA_FILE_TYPE_METADATA_KEY, fileType)
                .containsEntry(FileMetadataProvider.GRIDCAPA_FILE_NAME_METADATA_KEY, fileName)
                .containsEntry(FileMetadataProvider.GRIDCAPA_FILE_VALIDITY_INTERVAL_METADATA_KEY, fileValidityInterval)
                .containsEntry(FileMetadataProvider.GRIDCAPA_DOCUMENT_ID_METADATA_KEY, documentId);
    }
}
