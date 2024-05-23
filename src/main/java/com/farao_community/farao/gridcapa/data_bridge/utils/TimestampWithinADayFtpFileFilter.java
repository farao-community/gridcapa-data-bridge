/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.utils;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.file.filters.FileListFilter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * @author Daniel Thirion {@literal <daniel.thirion at rte-france.com>}
 */
public class TimestampWithinADayFtpFileFilter implements FileListFilter<FTPFile> {

    @Override
    public List<FTPFile> filterFiles(final FTPFile[] files) {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        final long oneDayAgo = cal.getTimeInMillis();
        return Arrays.stream(files)
                .filter(file -> file.getTimestamp().getTimeInMillis() > oneDayAgo)
                .toList();
    }
}
