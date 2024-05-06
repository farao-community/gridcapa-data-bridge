/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.gridcapa.data_bridge.configuration.DataBridgeConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class FileMetadataConfigurationTest {

    @Autowired
    private DataBridgeConfiguration dataBridgeConfiguration;

    @Test
    void testDefaultConfig() {
        assertEquals("CSE_D2CC", dataBridgeConfiguration.getTargetProcess());
        FileMetadataConfiguration file0 = dataBridgeConfiguration.getFiles().get(0);
        assertEquals("CGM", file0.fileType());
        assertEquals("regex_test", file0.fileRegex());
        assertEquals(".*.zip|[0-9]{8}_[0-9]{4}_.*.(uct|UCT)", file0.remoteFileRegex());
        assertTrue(file0.doUnzip());
        assertEquals("cgms", file0.sourceDirectory());
        assertEquals("CGMs", file0.sinkDirectory());
        FileMetadataConfiguration file1 = dataBridgeConfiguration.getFiles().get(1);
        assertEquals("CRAC", file1.fileType());
        assertEquals("regex_test", file1.fileRegex());
        assertEquals(".*Transit.*.zip|[0-9]{8}_[0-9]{4}_.*Transit.*.(xml|XML)", file1.remoteFileRegex());
        assertTrue(file1.doUnzip());
        assertEquals("cracs", file1.sourceDirectory());
        assertEquals("CRACs", file1.sinkDirectory());
    }
}
