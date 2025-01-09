/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.utils;

import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;

import java.nio.file.Path;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel  at rte-france.com>}
 */
public final class FtpInboundFileFilter {

    private FtpInboundFileFilter() {
        throw new AssertionError("Utility DataBridgeFtpInboundFileSynchronizer class should not be instantiated");
    }

    public static FileListFilter<FTPFile> ftpInboundFileFilter(FileMetadataConfiguration fileMetadataConfiguration)  {
        CompositeFileListFilter<FTPFile> fileListFilter = new CompositeFileListFilter<>();
        fileListFilter.addFilter(new FtpRegexPatternFileListFilter(fileMetadataConfiguration.remoteFileRegex()));
        fileListFilter.addFilter(new TimestampWithinADayFtpFileFilter());
        fileListFilter.addFilter(createFilePersistenceFilter(fileMetadataConfiguration));
        return fileListFilter;

    }

    private static FtpPersistentAcceptOnceFileListFilter createFilePersistenceFilter(FileMetadataConfiguration fileMetadataConfiguration) {
        ConcurrentMetadataStore metadataStore = createMetadataStoreForFilePersistence(fileMetadataConfiguration);
        FtpPersistentAcceptOnceFileListFilter ftpPersistentAcceptOnceFileListFilter = new FtpPersistentAcceptOnceFileListFilter(metadataStore, "");
        ftpPersistentAcceptOnceFileListFilter.setFlushOnUpdate(true);
        return ftpPersistentAcceptOnceFileListFilter;
    }

    private static ConcurrentMetadataStore createMetadataStoreForFilePersistence(FileMetadataConfiguration fileMetadataConfiguration) {
        Path persistenceFilePath = Path.of(fileMetadataConfiguration.fileListPersistenceFile());
        PropertiesPersistingMetadataStore filePersistenceMetadataStore = new PropertiesPersistingMetadataStore();
        filePersistenceMetadataStore.setBaseDirectory(persistenceFilePath.getParent().toString());
        filePersistenceMetadataStore.setFileName(persistenceFilePath.getFileName().toString());
        filePersistenceMetadataStore.afterPropertiesSet();
        return filePersistenceMetadataStore;
    }

}
