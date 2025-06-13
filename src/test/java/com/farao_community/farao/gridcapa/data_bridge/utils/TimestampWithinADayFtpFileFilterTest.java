/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.utils;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Daniel Thirion {@literal <daniel.thirion at rte-france.com>}
 */
class TimestampWithinADayFtpFileFilterTest {

    private TimestampWithinADayFtpFileFilter filter;
    private static final long NB_MS_IN_AN_HOUR = 60 * 60 * 1000;

    @BeforeEach
    void setUp() {
        filter = new TimestampWithinADayFtpFileFilter();
    }

    @Test
    void testFilterFilesEmptyArray() {
        final FTPFile[] files = new FTPFile[0];
        final List<FTPFile> result = filter.filterFiles(files);
        assertEquals(0, result.size());
    }

    @Test
    void someFilesWithinADay() {
        final FTPFile file1 = createMockFTPFileWithOffset(-25 * NB_MS_IN_AN_HOUR); // 25 hours ago
        final FTPFile file2 = createMockFTPFileWithOffset(-2 * NB_MS_IN_AN_HOUR);  // 2 hours ago
        final FTPFile[] files = {file1, file2};

        final List<FTPFile> result = filter.filterFiles(files);
        assertEquals(1, result.size());
        assertEquals(file2, result.get(0));
    }

    private FTPFile createMockFTPFileWithOffset(final long offsetMillis) {
        final FTPFile file = mock(FTPFile.class);
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTimeInMillis(System.currentTimeMillis() + offsetMillis);
        when(file.getTimestamp()).thenReturn(timestamp);
        return file;
    }
}
