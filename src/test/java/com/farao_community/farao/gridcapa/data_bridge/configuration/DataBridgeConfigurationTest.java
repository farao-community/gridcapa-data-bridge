/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.configuration;

import com.farao_community.farao.gridcapa.data_bridge.exception.DataBridgeException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class DataBridgeConfigurationTest {

    @Autowired
    private DataBridgeConfiguration configuration;

    @Test
    void testDefaultConfig() {
        Assertions.assertThat(configuration.getTargetProcess()).isEqualTo("CSE_D2CC");
        Assertions.assertThat(configuration.getZoneId()).isEqualTo("Europe/Paris");

        List<FileMetadataConfiguration> files = configuration.getFiles();

        Assertions.assertThat(files)
                .hasSize(7)
                .extracting(FileMetadataConfiguration::fileType,
                        FileMetadataConfiguration::fileRegex,
                        FileMetadataConfiguration::remoteFileRegex,
                        FileMetadataConfiguration::doUnzip,
                        FileMetadataConfiguration::sourceDirectory,
                        FileMetadataConfiguration::sinkDirectory)
                .contains(
                        Assertions.tuple(FileType.CBCORA, ".*F301.*.xml", ".*F301.*.xml", true, "cbcoras", "CBCORAs"),
                        Assertions.tuple(FileType.CGM, ".*F119.*.zip", ".*F119.*.zip", true, "cgms", "CGMs"),
                        Assertions.tuple(FileType.DCCGM, ".*F139.*.zip", ".*F139.*.zip", true, "dccgms", "DCCGMs"),
                        Assertions.tuple(FileType.GLSK, ".*F319.*.xml", ".*F319.*.xml", true, "glsks", "GLSKs"),
                        Assertions.tuple(FileType.RAOREQUEST, ".*F302.*.xml", ".*F302.*.xml", true, "raorequests", "RAOREQUESTs"),
                        Assertions.tuple(FileType.REFPROG, ".*F120.*.xml", ".*F120.*.xml", true, "refprogs", "REFPROGs"),
                        Assertions.tuple(FileType.VIRTUALHUB, ".*F327.*.xml", ".*F327.*.xml", false, "virtualhubs", "VIRTUALHUBs"));
    }

    @Test
    void getFileConfigurationFromNameTest() {
        FileMetadataConfiguration fileConfiguration = configuration.getFileConfigurationFromName("test-F120.xml");
        Assertions.assertThat(fileConfiguration.fileType()).isEqualTo(FileType.REFPROG);
    }

    @Test
    void getFileConfigurationFromNameNotFoundTest() {
        Assertions.assertThatExceptionOfType(DataBridgeException.class)
                .isThrownBy(() -> configuration.getFileConfigurationFromName("nothing_like_that.txt"));
    }

    @Test
    void getFileConfigurationFromRemoteNameTest() {
        FileMetadataConfiguration fileConfiguration = configuration.getFileConfigurationFromRemoteName("test-F119.zip");
        Assertions.assertThat(fileConfiguration.fileType()).isEqualTo(FileType.CGM);
    }

    @Test
    void getFileConfigurationFromRemoteNameNotFoundTest() {
        Assertions.assertThatExceptionOfType(DataBridgeException.class)
                .isThrownBy(() -> configuration.getFileConfigurationFromRemoteName("nothing_like_that.txt"));
    }
}
