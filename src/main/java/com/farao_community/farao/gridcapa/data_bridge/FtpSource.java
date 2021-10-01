/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.messaging.MessageChannel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
public class FtpSource {
    public static final String SYNCHRONIZE_TEMP_DIRECTORY_PREFIX = "gridcapa-data-bridge";

    private final ApplicationContext applicationContext;

    @Value("${data-bridge.sources.ftp.host}")
    private String host;
    @Value("${data-bridge.sources.ftp.port}")
    private int port;
    @Value("${data-bridge.sources.ftp.username}")
    private String username;
    @Value("${data-bridge.sources.ftp.password}")
    private String password;
    @Value("${data-bridge.sources.ftp.base-directory}")
    private String baseDirectory;

    public FtpSource(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public MessageChannel ftpSourceChannel() {
        return new PublishSubscribeChannel();
    }

    private SessionFactory<FTPFile> ftpSessionFactory() {
        DefaultFtpSessionFactory sf = new DefaultFtpSessionFactory();
        sf.setHost(host);
        sf.setPort(port);
        sf.setUsername(username);
        sf.setPassword(password);
        sf.setClientMode(FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE);
        return sf;
    }

    public FtpInboundFileSynchronizer ftpInboundFileSynchronizer() {
        FtpInboundFileSynchronizer fileSynchronizer = new FtpInboundFileSynchronizer(ftpSessionFactory());
        fileSynchronizer.setDeleteRemoteFiles(false);
        fileSynchronizer.setBeanFactory(applicationContext);
        fileSynchronizer.setRemoteDirectory(baseDirectory);
        fileSynchronizer.setFilter(Arrays::asList);
        return fileSynchronizer;
    }

    @Bean
    @InboundChannelAdapter(channel = "ftpSourceChannel", poller = @Poller(fixedDelay = "${data-bridge.sources.ftp.polling-delay-in-ms}"))
    public MessageSource<File> ftpMessageSource() throws IOException {
        FtpInboundFileSynchronizingMessageSource source =
                new FtpInboundFileSynchronizingMessageSource(ftpInboundFileSynchronizer());
        source.setLocalDirectory(Files.createTempDirectory(SYNCHRONIZE_TEMP_DIRECTORY_PREFIX).toFile());
        source.setAutoCreateLocalDirectory(true);
        return source;
    }

    @Bean
    public IntegrationFlow ftpPreprocessFlow() {
        return IntegrationFlows.from("ftpSourceChannel")
                .channel("archivesChannel")
                .get();
    }
}
