package com.farao_community.farao.gridcapa.data_bridge.utils;

import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.SftpConfiguration;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;

import java.nio.file.Path;

public final class SftpInboundFileFilter {

    private SftpInboundFileFilter() {
        throw new AssertionError("Utility DataBridgeFtpInboundFileSynchronizer class should not be instantiated");
    }

    public static FileListFilter sftpInboundFileFilter(SftpConfiguration sftpConfiguration, FileMetadataConfiguration fileMetadataConfiguration)  {
        CompositeFileListFilter fileListFilter = new CompositeFileListFilter();
        fileListFilter.addFilter(new SftpRegexPatternFileListFilter(fileMetadataConfiguration.remoteFileRegex()));
        fileListFilter.addFilter(createFilePersistenceFilter(sftpConfiguration));
        return fileListFilter;

    }

    private static SftpPersistentAcceptOnceFileListFilter createFilePersistenceFilter(SftpConfiguration sftpConfiguration) {
        ConcurrentMetadataStore metadataStore = createMetadataStoreForFilePersistence(sftpConfiguration);
        SftpPersistentAcceptOnceFileListFilter sftpPersistentAcceptOnceFileListFilter = new SftpPersistentAcceptOnceFileListFilter(metadataStore, "");
        sftpPersistentAcceptOnceFileListFilter.setFlushOnUpdate(true);
        return sftpPersistentAcceptOnceFileListFilter;
    }

    private static ConcurrentMetadataStore createMetadataStoreForFilePersistence(SftpConfiguration sftpConfiguration) {
        Path persistenceFilePath = Path.of(sftpConfiguration.getFileListPersistenceFile());
        PropertiesPersistingMetadataStore filePersistenceMetadataStore = new PropertiesPersistingMetadataStore();
        filePersistenceMetadataStore.setBaseDirectory(persistenceFilePath.getParent().toString());
        filePersistenceMetadataStore.setFileName(persistenceFilePath.getFileName().toString());
        filePersistenceMetadataStore.afterPropertiesSet();
        return filePersistenceMetadataStore;
    }

}
