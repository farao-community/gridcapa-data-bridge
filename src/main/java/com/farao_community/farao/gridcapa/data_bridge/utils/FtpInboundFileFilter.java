package com.farao_community.farao.gridcapa.data_bridge.utils;

import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FtpConfiguration;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;

import java.nio.file.Path;

public final class FtpInboundFileFilter {

    private FtpInboundFileFilter() {
        throw new AssertionError("Utility DataBridgeFtpInboundFileSynchronizer class should not be instantiated");
    }

    public static FileListFilter ftpInboundFileFilter(FtpConfiguration ftpConfiguration, FileMetadataConfiguration fileMetadataConfiguration)  {
        CompositeFileListFilter fileListFilter = new CompositeFileListFilter();
        fileListFilter.addFilter(new FtpRegexPatternFileListFilter(fileMetadataConfiguration.remoteFileRegex()));
        fileListFilter.addFilter(createFilePersistenceFilter(ftpConfiguration));
        return fileListFilter;

    }

    private static FtpPersistentAcceptOnceFileListFilter createFilePersistenceFilter(FtpConfiguration ftpConfiguration) {
        ConcurrentMetadataStore metadataStore = createMetadataStoreForFilePersistence(ftpConfiguration);
        FtpPersistentAcceptOnceFileListFilter ftpPersistentAcceptOnceFileListFilter = new FtpPersistentAcceptOnceFileListFilter(metadataStore, "");
        ftpPersistentAcceptOnceFileListFilter.setFlushOnUpdate(true);
        return ftpPersistentAcceptOnceFileListFilter;
    }

    private static ConcurrentMetadataStore createMetadataStoreForFilePersistence(FtpConfiguration ftpConfiguration) {
        Path persistenceFilePath = Path.of(ftpConfiguration.getFileListPersistenceFile());
        PropertiesPersistingMetadataStore filePersistenceMetadataStore = new PropertiesPersistingMetadataStore();
        filePersistenceMetadataStore.setBaseDirectory(persistenceFilePath.getParent().toString());
        filePersistenceMetadataStore.setFileName(persistenceFilePath.getFileName().toString());
        filePersistenceMetadataStore.afterPropertiesSet();
        return filePersistenceMetadataStore;
    }

}
