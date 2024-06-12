/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.minio_adapter.starter.MinioAdapterConstants;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.io.File;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class FileTransferFlowTest {
    @Autowired
    private FileTransferFlow flow;

    @ParameterizedTest
    @CsvSource({
        "test-F119.zip,00AAAA-AA------A-20240610F119v1,CGMs",
        "test-F139.zip,00AAAA-AA------A-20240610F139v1,DCCGMs",
        "test-F120.xml,00AAAA-AA------A-20240610F120v1,REFPROGs",
        "test-F301.xml,00AAAA-AA------A-20240610F301v3,CBCORAs",
        "test-F319.xml,00AAAA-AA------A-20240610F319v1,GLSKs"
    })
    void addHeadersTest(String filename, String documentId, String fileSink) {
        File testFile = new File(getClass().getResource("/inputs/" + filename).getFile());
        Message<File> initialMessage = MessageBuilder.withPayload(testFile)
                .setHeader("file_name", filename)
                .build();

        Message<File> fileMessage = flow.addHeaders(initialMessage);

        Assertions.assertThat(fileMessage.getHeaders().get(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY))
                .isNotNull()
                .isEqualTo(filename);
        Assertions.assertThat(fileMessage.getHeaders().get("file_sink"))
                .isNotNull()
                .isEqualTo(fileSink);
        Assertions.assertThat(fileMessage.getHeaders().get(MinioAdapterConstants.DEFAULT_GRIDCAPA_DOCUMENT_ID_METADATA_KEY))
                .isNotNull()
                .isEqualTo(documentId);
        Assertions.assertThat(fileMessage.getPayload())
                .isNotNull()
                .isEqualTo(testFile);
    }

    @Test
    void addHeadersVirtualHubsTest() {
        String filename = "test-F327.xml";
        File testFile = new File(getClass().getResource("/inputs/" + filename).getFile());
        Message<File> initialMessage = MessageBuilder.withPayload(testFile)
                .setHeader("file_name", filename)
                .build();

        Message<File> fileMessage = flow.addHeaders(initialMessage);

        Assertions.assertThat(fileMessage.getHeaders().get(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY))
                .isNotNull()
                .isEqualTo(filename);
        Assertions.assertThat(fileMessage.getHeaders().get("file_sink"))
                .isNotNull()
                .isEqualTo("VIRTUALHUBs");
        Assertions.assertThat(fileMessage.getHeaders().get(MinioAdapterConstants.DEFAULT_GRIDCAPA_DOCUMENT_ID_METADATA_KEY))
                .isNull();
        Assertions.assertThat(fileMessage.getPayload())
                .isNotNull()
                .isEqualTo(testFile);
    }

    @Test
    void addHeadersInvalidContentTest() {
        String filename = "test-F120-bad-content.xml";
        File testFile = new File(getClass().getResource("/inputs/" + filename).getFile());
        Message<File> initialMessage = MessageBuilder.withPayload(testFile)
                .setHeader("file_name", filename)
                .build();

        Message<File> fileMessage = flow.addHeaders(initialMessage);

        Assertions.assertThat(fileMessage.getHeaders())
                .isNotNull()
                .containsEntry("file_name", filename)
                .containsKeys("errorChannel", "cause");
    }
}
