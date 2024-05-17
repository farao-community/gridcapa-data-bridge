package com.farao_community.farao.gridcapa.data_bridge.sources;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.file.filters.FileListFilter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

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
