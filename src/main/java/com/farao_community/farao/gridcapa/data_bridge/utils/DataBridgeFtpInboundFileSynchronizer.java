package com.farao_community.farao.gridcapa.data_bridge.utils;

import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FtpConfiguration;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;

import java.nio.file.Path;

public final class DataBridgeFtpInboundFileSynchronizer {

    private DataBridgeFtpInboundFileSynchronizer() {
        throw new AssertionError("Utility DataBridgeFtpInboundFileSynchronizer class should not be instantiated");
    }

    public static FtpInboundFileSynchronizer ftpInboundFileSynchronizer(SessionFactory<FTPFile> sessionFactory, ApplicationContext applicationContext, FtpConfiguration ftpConfiguration, FileMetadataConfiguration fileMetadataConfiguration)  {
        FtpInboundFileSynchronizer fileSynchronizer = new FtpInboundFileSynchronizer(sessionFactory);
        fileSynchronizer.setDeleteRemoteFiles(false);
        fileSynchronizer.setBeanFactory(applicationContext);
        fileSynchronizer.setRemoteDirectory(ftpConfiguration.getBaseDirectory() + fileMetadataConfiguration.sourceDirectory());
        fileSynchronizer.setPreserveTimestamp(true);
        CompositeFileListFilter fileListFilter = new CompositeFileListFilter();
        fileListFilter.addFilter(new FtpRegexPatternFileListFilter(fileMetadataConfiguration.remoteFileRegex()));
        fileListFilter.addFilter(createFilePersistenceFilter(ftpConfiguration));
        fileSynchronizer.setFilter(fileListFilter);
        return fileSynchronizer;
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
