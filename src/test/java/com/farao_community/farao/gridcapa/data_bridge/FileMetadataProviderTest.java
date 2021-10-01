/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@SpringBootTest
class FileMetadataProviderTest {
    @Autowired
    private FileMetadataProvider fileMetadataProvider;

    @Test
    void checkMetadataSetCorrectlyWhenUcteFileIsCorrect() {
        Message<?> ucteFileMessage = MessageBuilder
                .withPayload("")
                .setHeader("gridcapa_file_name", "20210101_1430_2D5_CSE1.uct")
                .build();
        Map<String, String> metadataMap = new HashMap<>();
        fileMetadataProvider.populateMetadata(ucteFileMessage, metadataMap);

        assertEquals("CSE_D2CC", metadataMap.get(fileMetadataProvider.GRIDCAPA_TARGET_PROCESS_METADATA_KEY));
        assertEquals("CGM", metadataMap.get(fileMetadataProvider.GRIDCAPA_FILE_TYPE_METADATA_KEY));
    }
}
