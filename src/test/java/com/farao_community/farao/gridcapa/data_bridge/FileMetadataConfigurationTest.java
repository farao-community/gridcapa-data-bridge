/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

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
    private FileMetadataConfiguration fileMetadataConfiguration;

    @Test
    void testDefaultConfig() {
        assertEquals("CGM", fileMetadataConfiguration.fileType());
        assertEquals("regex_test", fileMetadataConfiguration.fileRegex());
        assertEquals("CSE_D2CC", fileMetadataConfiguration.getTargetProcess());
        assertEquals("hourly", fileMetadataConfiguration.timeValidity());
    }
}
