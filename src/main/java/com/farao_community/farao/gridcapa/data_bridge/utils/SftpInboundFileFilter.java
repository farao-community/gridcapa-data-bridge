/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.utils;

import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;

import java.nio.file.Path;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel  at rte-france.com>}
 */
public final class SftpInboundFileFilter {

    private SftpInboundFileFilter() {
        throw new AssertionError("Utility DataBridgeFtpInboundFileSynchronizer class should not be instantiated");
    }

    public static FileListFilter<SftpClient.DirEntry> sftpInboundFileFilter(FileMetadataConfiguration fileMetadataConfiguration)  {
        CompositeFileListFilter<SftpClient.DirEntry> fileListFilter = new CompositeFileListFilter<>();
        fileListFilter.addFilter(new SftpRegexPatternFileListFilter(fileMetadataConfiguration.remoteFileRegex()));
        fileListFilter.addFilter(createFilePersistenceFilter(fileMetadataConfiguration));
        return fileListFilter;

    }

    private static SftpPersistentAcceptOnceFileListFilter createFilePersistenceFilter(FileMetadataConfiguration fileMetadataConfiguration) {
        ConcurrentMetadataStore metadataStore = createMetadataStoreForFilePersistence(fileMetadataConfiguration);
        SftpPersistentAcceptOnceFileListFilter sftpPersistentAcceptOnceFileListFilter = new SftpPersistentAcceptOnceFileListFilter(metadataStore, "");
        sftpPersistentAcceptOnceFileListFilter.setFlushOnUpdate(true);
        return sftpPersistentAcceptOnceFileListFilter;
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
