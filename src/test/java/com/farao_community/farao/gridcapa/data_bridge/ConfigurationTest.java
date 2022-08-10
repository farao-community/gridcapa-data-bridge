package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.gridcapa.data_bridge.sources.RemoteFileConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConfigurationTest {

    @Autowired
    private RemoteFileConfiguration remoteFileConfiguration;

    @Test
    void checkReadConfiguration() {

        assertEquals(1, remoteFileConfiguration.getDataBridgeList().size());
        assertEquals("Europe/Paris", remoteFileConfiguration.getZoneId());
        assertEquals("Europe/Paris", remoteFileConfiguration.getDataBridgeList().get(0).getZoneId());
        assertEquals("CSE_D2CC", remoteFileConfiguration.getDataBridgeList().get(0).getTargetProcess());
        assertEquals("CGM", remoteFileConfiguration.getDataBridgeList().get(0).getFileType());
        assertEquals("regex_test", remoteFileConfiguration.getDataBridgeList().get(0).getFileRegex());
        assertEquals("hourly", remoteFileConfiguration.getDataBridgeList().get(0).getTimeValidity());
        assertEquals("/data/gridcapa/cse/d2cc/cgms", remoteFileConfiguration.getDataBridgeList().get(0).getFtpDirectory());
        assertEquals("/cgms", remoteFileConfiguration.getDataBridgeList().get(0).getMinioDirectory());
    }
}
