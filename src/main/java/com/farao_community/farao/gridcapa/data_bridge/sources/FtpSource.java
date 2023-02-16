/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.MessageChannel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
@ConditionalOnProperty(prefix = "data-bridge.sources.ftp", name = "active", havingValue = "true")
public class FtpSource {
    private static final String SYNCHRONIZE_TEMP_DIRECTORY_PREFIX = "gridcapa-data-bridge";

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final ApplicationContext applicationContext;
    private final RemoteFileConfiguration remoteFileConfiguration;

    @Value("${data-bridge.sources.ftp.host}")
    private String ftpHost;
    @Value("${data-bridge.sources.ftp.port}")
    private int ftpPort;
    @Value("${data-bridge.sources.ftp.username}")
    private String ftpUsername;
    @Value("${data-bridge.sources.ftp.password}")
    private String ftpPassword;
    @Value("${data-bridge.sources.ftp.base-directory}")
    private String ftpBaseDirectory;
    @Value("${data-bridge.sources.ftp.file-list-persistence-file:/tmp/gridcapa/ftp-metadata-store.properties}")
    private String fileListPersistenceFile;
    @Value("${data-bridge.sources.ftp.data-timeout:60000}")
    private int ftpDataTimeout;

    public FtpSource(ApplicationContext applicationContext, RemoteFileConfiguration remoteFileConfiguration) {
        this.applicationContext = applicationContext;
        this.remoteFileConfiguration = remoteFileConfiguration;
    }

    @Bean
    public MessageChannel ftpSourceChannel() {
        return new PublishSubscribeChannel();
    }

    private SessionFactory<FTPFile> ftpSessionFactory() {
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setHost(ftpHost);
        ftpSessionFactory.setPort(ftpPort);
        ftpSessionFactory.setUsername(ftpUsername);
        ftpSessionFactory.setPassword(ftpPassword);
        ftpSessionFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        ftpSessionFactory.setDataTimeout(ftpDataTimeout);
        return ftpSessionFactory;
    }

    private ConcurrentMetadataStore createMetadataStoreForFilePersistence() {
        Path persistenceFilePath = Path.of(fileListPersistenceFile);
        PropertiesPersistingMetadataStore filePersistenceMetadataStore = new PropertiesPersistingMetadataStore();
        filePersistenceMetadataStore.setBaseDirectory(persistenceFilePath.getParent().toString());
        filePersistenceMetadataStore.setFileName(persistenceFilePath.getFileName().toString());
        filePersistenceMetadataStore.afterPropertiesSet();
        return filePersistenceMetadataStore;
    }

    private FtpPersistentAcceptOnceFileListFilter createFilePersistenceFilter() {
        ConcurrentMetadataStore metadataStore = createMetadataStoreForFilePersistence();
        FtpPersistentAcceptOnceFileListFilter ftpPersistentAcceptOnceFileListFilter = new FtpPersistentAcceptOnceFileListFilter(metadataStore, "");
        ftpPersistentAcceptOnceFileListFilter.setFlushOnUpdate(true);
        return ftpPersistentAcceptOnceFileListFilter;
    }

    private FtpInboundFileSynchronizer ftpInboundFileSynchronizer() {
        FtpInboundFileSynchronizer fileSynchronizer = new FtpInboundFileSynchronizer(ftpSessionFactory());
        fileSynchronizer.setDeleteRemoteFiles(false);
        fileSynchronizer.setBeanFactory(applicationContext);
        fileSynchronizer.setRemoteDirectory(ftpBaseDirectory);
        fileSynchronizer.setPreserveTimestamp(true);
        CompositeFileListFilter fileListFilter = new CompositeFileListFilter();
        fileListFilter.addFilter(new FtpRegexPatternFileListFilter(String.join("|", remoteFileConfiguration.getRemoteFileRegex())));
        fileListFilter.addFilter(createFilePersistenceFilter());
        fileSynchronizer.setFilter(fileListFilter);
        return fileSynchronizer;
    }

    @Bean
    @InboundChannelAdapter(channel = "ftpSourceChannel", poller = @Poller(fixedDelay = "${data-bridge.sources.ftp.polling-delay-in-ms}"))
    public MessageSource<File> ftpMessageSource() throws IOException {
        FtpInboundFileSynchronizingMessageSource source =
                new FtpInboundFileSynchronizingMessageSource(ftpInboundFileSynchronizer());
        source.setLocalDirectory(Files.createTempDirectory(SYNCHRONIZE_TEMP_DIRECTORY_PREFIX).toFile());
        source.setAutoCreateLocalDirectory(true);
        source.setLocalFilter(new FileSystemPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), ""));
        return source;
    }

    @Bean
    public IntegrationFlow ftpPreprocessFlow() {
        return IntegrationFlows.from("ftpSourceChannel")
                .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"ftp treatment of file \" + headers.file_name"))
                .channel("archivesChannel")
                .get();
    }
}
