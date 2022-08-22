/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;

import com.farao_community.farao.gridcapa.data_bridge.model.DataBridge;
import com.jcraft.jsch.ChannelSftp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.dsl.SftpInboundChannelAdapterSpec;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
@ConditionalOnProperty(prefix = "data-bridges.sftp", name = "active", havingValue = "true")
public class SftpSource {
    public static final String SYNCHRONIZE_TEMP_DIRECTORY_PREFIX = "gridcapa-data-bridge";

    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final RemoteFileConfiguration remoteFilesConfiguration;

    @Value("${data-bridges.sftp.host}")
    private String sftpHost;
    @Value("${data-bridges.sftp.port}")
    private int sftpPort;
    @Value("${data-bridges.sftp.username}")
    private String sftpUsername;
    @Value("${data-bridges.sftp.password}")
    private String sftpPassword;

    @Value("${data-bridges.sftp.polling-delay-in-ms}")
    private Integer polling;

    public SftpSource(AutowireCapableBeanFactory autowireCapableBeanFactory, RemoteFileConfiguration remoteFilesConfiguration) {
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.remoteFilesConfiguration = remoteFilesConfiguration;
    }

    private DefaultSftpSessionFactory sftpSessionFactory() {
        DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
        sftpSessionFactory.setHost(sftpHost);
        sftpSessionFactory.setPort(sftpPort);
        sftpSessionFactory.setUser(sftpUsername);
        sftpSessionFactory.setPassword(sftpPassword);
        sftpSessionFactory.setAllowUnknownKeys(true);
        sftpSessionFactory.setServerAliveInterval(100);
        return sftpSessionFactory;
    }

    @PostConstruct
    public void allSFTP() throws IOException {
        for (DataBridge bridge : remoteFilesConfiguration.getDataBridgeList()) {
            IntegrationFlow ms = this.sftpInboundFlow(bridge);
            this.autowireCapableBeanFactory.initializeBean(ms, bridge.getBridgeIdentifiant() + "_sftp_adapter");
        }
    }

    public IntegrationFlow sftpInboundFlow(DataBridge bridge) throws IOException {
        CompositeFileListFilter<ChannelSftp.LsEntry> fileListFilter = new CompositeFileListFilter<>();
        fileListFilter.addFilter(new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), ""));
        fileListFilter.addFilter(new SftpRegexPatternFileListFilter(String.join("|", bridge.getRemoteFileRegex())));
        SftpInboundChannelAdapterSpec sftpInboundChannelAdapterSpec = Sftp.inboundAdapter(sftpSessionFactory())
                .preserveTimestamp(true)
                .deleteRemoteFiles(false)
                .remoteDirectory(bridge.getFtpDirectory())
                .filter(fileListFilter)
                .autoCreateLocalDirectory(true)
                .localFilter(new FileSystemPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), ""))
                .localDirectory(Files.createTempDirectory(SYNCHRONIZE_TEMP_DIRECTORY_PREFIX).toFile());

        Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer = e -> e.id(bridge.getBridgeIdentifiant() + "_sftp")
                .autoStartup(true)
                .poller(Pollers.fixedDelay(polling));

        return IntegrationFlows
                .from(sftpInboundChannelAdapterSpec, endpointConfigurer)
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
