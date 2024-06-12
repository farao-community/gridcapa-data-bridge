/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.health;

import com.farao_community.farao.gridcapa.data_bridge.configuration.FtpConfiguration;
import org.apache.commons.net.ftp.FTPFile;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;

import java.io.IOException;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class FtpHealthIndicatorTest {

    private final SessionFactory<FTPFile> ftpSessionFactory = Mockito.mock(SessionFactory.class);
    private final FtpConfiguration ftpConfiguration = Mockito.mock(FtpConfiguration.class);
    private final FtpHealthIndicator healthIndicator = new FtpHealthIndicator(ftpSessionFactory, ftpConfiguration);

    @ParameterizedTest
    @CsvSource({
        "true,true,UP",
        "true,false,DOWN", // Directory does not exist
        "false,true,DOWN"  // No connection
    })
    void healthTest(boolean sessionTest, boolean sessionExists, String status) throws IOException {
        String baseDirectory = "baseDirectory";
        Session<FTPFile> session = Mockito.mock(Session.class);
        Mockito.when(ftpSessionFactory.getSession()).thenReturn(session);
        Mockito.when(session.test()).thenReturn(sessionTest);
        Mockito.when(ftpConfiguration.getBaseDirectory()).thenReturn(baseDirectory);
        Mockito.when(session.exists(baseDirectory)).thenReturn(sessionExists);

        Health healthResult = healthIndicator.health();

        Assertions.assertThat(healthResult.getStatus().getCode()).isEqualTo(status);
    }

    @Test
    void healthDownExceptionTest() throws IOException {
        String baseDirectory = "baseDirectory";
        Session<FTPFile> session = Mockito.mock(Session.class);
        Mockito.when(ftpSessionFactory.getSession()).thenReturn(session);
        Mockito.when(session.test()).thenReturn(true);
        Mockito.when(ftpConfiguration.getBaseDirectory()).thenReturn(baseDirectory);
        Mockito.when(session.exists(baseDirectory)).thenThrow(new IOException());

        Health healthResult = healthIndicator.health();

        Assertions.assertThat(healthResult.getStatus().getCode()).isEqualTo("DOWN");
    }
}
