/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.MessageChannel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@Configuration
@ConditionalOnProperty(prefix = "data-bridge.sources.sftp", name = "active", havingValue = "true")
public class SftpSource {
    public static final String SYNCHRONIZE_TEMP_DIRECTORY_PREFIX = "gridcapa-data-bridge";

    @Value("${data-bridge.sources.sftp.host}")
    private String sftpHost;
    @Value("${data-bridge.sources.sftp.port}")
    private int sftpPort;
    @Value("${data-bridge.sources.sftp.username}")
    private String sftpUsername;
    @Value("${data-bridge.sources.sftp.password}")
    private String sftpPassword;
    @Value("${data-bridge.sources.sftp.base-directory}")
    private String sftpBaseDirectory;

    @Bean
    public MessageChannel sftpSourceChannel() {
        return new PublishSubscribeChannel();
    }

    private DefaultSftpSessionFactory sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(sftpHost);
        factory.setPort(sftpPort);
        factory.setUser(sftpUsername);
        factory.setPassword(sftpPassword);
        factory.setAllowUnknownKeys(true);
        return factory;
    }

    private SftpInboundFileSynchronizer sftpInboundFileSynchronizer() {
        SftpInboundFileSynchronizer synchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
        synchronizer.setDeleteRemoteFiles(false);
        synchronizer.setRemoteDirectory(sftpBaseDirectory);
        return synchronizer;
    }

    @Bean
    @InboundChannelAdapter(channel = "sftpSourceChannel", poller = @Poller(fixedDelay = "${data-bridge.sources.sftp.polling-delay-in-ms}"))
    public MessageSource<File> sftpMessageSource() throws IOException {
        SftpInboundFileSynchronizingMessageSource source = new SftpInboundFileSynchronizingMessageSource(sftpInboundFileSynchronizer());
        source.setLocalDirectory(Files.createTempDirectory(SYNCHRONIZE_TEMP_DIRECTORY_PREFIX).toFile());
        source.setAutoCreateLocalDirectory(true);
        return source;
    }

    @Bean
    public IntegrationFlow sftpPreprocessFlow() {
        return IntegrationFlows.from("sftpSourceChannel")
                .channel("archivesChannel")
                .get();
    }
}
