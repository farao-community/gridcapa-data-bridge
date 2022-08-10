/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;

import com.farao_community.farao.gridcapa.data_bridge.model.DataBridge;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
@ConditionalOnProperty(prefix = "data-bridges.ftp", name = "active", havingValue = "true")
public class FtpSource {
    public static final String SYNCHRONIZE_TEMP_DIRECTORY_PREFIX = "gridcapa-data-bridge";

    private final ApplicationContext applicationContext;
    private final RemoteFileConfiguration remoteFilesConfiguration;

    @Value("${data-bridges.ftp.host}")
    private String ftpHost;
    @Value("${data-bridges.ftp.port}")
    private int ftpPort;
    @Value("${data-bridges.ftp.username}")
    private String ftpUsername;
    @Value("${data-bridges.ftp.password}")
    private String ftpPassword;
    @Value("${data-bridges.ftp.polling-delay-in-ms}")
    private Integer polling;

    public FtpSource(ApplicationContext applicationContext, RemoteFileConfiguration remoteFilesConfiguration) {
        this.applicationContext = applicationContext;
        this.remoteFilesConfiguration = remoteFilesConfiguration;
    }

    private SessionFactory<FTPFile> ftpSessionFactory() {
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setHost(ftpHost);
        ftpSessionFactory.setPort(ftpPort);
        ftpSessionFactory.setUsername(ftpUsername);
        ftpSessionFactory.setPassword(ftpPassword);
        ftpSessionFactory.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        return ftpSessionFactory;
    }

    @PostConstruct
    public void allFTP() throws IOException {
        for (DataBridge bridge : remoteFilesConfiguration.getDataBridgeList()) {

            MessageChannel channel = new PublishSubscribeChannel();
            this.applicationContext.getAutowireCapableBeanFactory().initializeBean(channel, bridge.getBridgeIdentifiant() + "_ftp");

            IntegrationFlow flow = IntegrationFlows.from(bridge.getBridgeIdentifiant() + "_flow")
                    .channel("archivesChannel")
                    .get();

            IntegrationFlow ms = this.ftpInboundFlow(bridge);
            this.applicationContext.getAutowireCapableBeanFactory().initializeBean(ms, bridge.getBridgeIdentifiant() + "_adapter");

            this.applicationContext.getAutowireCapableBeanFactory().initializeBean(flow, bridge.getBridgeIdentifiant() + "_flow");

        }
    }

    public IntegrationFlow ftpInboundFlow(DataBridge bridge) throws IOException {
        CompositeFileListFilter<FTPFile> fileListFilter = new CompositeFileListFilter<>();
        fileListFilter.addFilter(new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), ""));
        fileListFilter.addFilter(new FtpRegexPatternFileListFilter(String.join("|", bridge.getRemoteFileRegex())));
        return IntegrationFlows
                .from(Ftp.inboundAdapter(ftpSessionFactory())
                                .preserveTimestamp(true)
                                .deleteRemoteFiles(false)
                                .remoteDirectory(bridge.getFtpDirectory())
                                .filter(fileListFilter)
                                .autoCreateLocalDirectory(true)
                                .localFilter(new FileSystemPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), ""))
                                .localDirectory(Files.createTempDirectory(SYNCHRONIZE_TEMP_DIRECTORY_PREFIX).toFile()),
                    e -> e.id(bridge.getBridgeIdentifiant() + "_ftp")
                                .autoStartup(true)
                                .poller(Pollers.fixedDelay(polling))

                )
                //.handle(m -> System.out.println(m.getPayload()))
                .transform(Message.class, m -> this.addDestination(m, bridge))
                .channel(bridge.getBridgeIdentifiant() + "_archives_channel")
                .get();
    }

    private Object addDestination(Message<File> message, DataBridge bridge) {
        return MessageBuilder.fromMessage(message)
                .setHeader("minio-base-directory", bridge.getMinioDirectory())
                .setHeader("file-validity", bridge.getTimeValidity())
                .setHeader("file-pattern", bridge.getFileRegex())
                .setHeader("target-process", bridge.getTargetProcess())
                .setHeader("file-type", bridge.getFileType())
                .setHeader("zone", bridge.getZoneId())
                .build();
    }

}
