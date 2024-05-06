/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;

import com.farao_community.farao.gridcapa.data_bridge.configuration.FtpConfiguration;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
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
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
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

    private final FtpConfiguration ftpConfiguration;

    public FtpSource(ApplicationContext applicationContext, FtpConfiguration ftpConfiguration) {
        this.applicationContext = applicationContext;
        this.ftpConfiguration = ftpConfiguration;
    }

    @Bean
    public MessageChannel ftpSourceChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public SessionFactory<FTPFile> ftpSessionFactory() {
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setHost(ftpConfiguration.getHost());
        ftpSessionFactory.setPort(ftpConfiguration.getPort());
        ftpSessionFactory.setUsername(ftpConfiguration.getUsername());
        ftpSessionFactory.setPassword(ftpConfiguration.getPassword());
        ftpSessionFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        ftpSessionFactory.setDataTimeout(ftpConfiguration.getDataTimeout());
        return ftpSessionFactory;
    }

    private ConcurrentMetadataStore createMetadataStoreForFilePersistence() {
        Path persistenceFilePath = Path.of(ftpConfiguration.getFileListPersistenceFile());
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
        fileSynchronizer.setRemoteDirectory(ftpConfiguration.getBaseDirectory());//TODO
        fileSynchronizer.setPreserveTimestamp(true);
        CompositeFileListFilter fileListFilter = new CompositeFileListFilter();
 //TODO       fileListFilter.addFilter(new FtpRegexPatternFileListFilter(String.join("|", remoteFileConfiguration.getRemoteFileRegex())));
        fileListFilter.addFilter(createFilePersistenceFilter());
        fileSynchronizer.setFilter(fileListFilter);
        return fileSynchronizer;
    }

    //
    @Bean
    @InboundChannelAdapter(channel = "ftpSourceChannel", poller = @Poller(fixedDelay = "${data-bridge.sources.ftp.polling-delay-in-ms}", maxMessagesPerPoll = "${data-bridge.sources.ftp.max-messages-per-poll}"))
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
        return IntegrationFlow.from("ftpSourceChannel")
                .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"ftp treatment of file \" + headers.file_name"))
                .channel("archivesChannel")
                .get();
    }
}
