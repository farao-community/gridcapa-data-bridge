/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FtpSourceTest {

    @Test
    void checkftpFileComparator() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2023, Calendar.AUGUST, 11, 14, 53, 55);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2023, Calendar.AUGUST, 11, 14, 53, 56);
        FTPFile file1 = new FTPFile();
        file1.setTimestamp(cal1);
        FTPFile file2 = new FTPFile();
        file2.setTimestamp(cal2);

        Comparator<FTPFile> comparator = ReflectionTestUtils.invokeMethod(FtpSource.class, "ftpFileTimestampComparator");

        assertTrue(comparator.compare(file1, file2) > 0);
        assertTrue(comparator.compare(file2, file1) < 0);
        assertEquals(0, comparator.compare(file2, file2));

        List<FTPFile> toBeSorted = new ArrayList<>();
        toBeSorted.add(file1);
        toBeSorted.add(file2);
        List<FTPFile> sorted = toBeSorted.stream().sorted(comparator).toList();
        assertEquals(file2, sorted.get(0));
        assertEquals(file1, sorted.get(1));
    }
}
