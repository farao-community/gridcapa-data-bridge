/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;

import com.farao_community.farao.gridcapa.data_bridge.DataBridgeException;
import com.farao_community.farao.gridcapa.data_bridge.configuration.DataBridgeConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.SftpConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.utils.SftpInboundFileFilter;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.dsl.Sftp;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel  at rte-france.com>}
 */
public class SftpDynamicBeanCreator implements BeanDefinitionRegistryPostProcessor {

    private static final String SYNCHRONIZE_TEMP_DIRECTORY_PREFIX = "gridcapa-core-cc-data-bridge";
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final List<FileMetadataConfiguration> fileMetadataConfigurations;
    private final SftpConfiguration sftpConfiguration;
    private final SessionFactory<SftpClient.DirEntry> sftpSessionFactory;

    public SftpDynamicBeanCreator(SessionFactory<SftpClient.DirEntry> sftpSessionFactory, ConfigurableEnvironment environment) {
        this.sftpConfiguration = Binder.get(environment)
                .bind("data-bridge.sources.sftp", Bindable.of(SftpConfiguration.class))
                .orElseThrow(() -> new DataBridgeException("Unable to create sftpSessionFactory: missing sftp config"));
        DataBridgeConfiguration dataBridgeConfiguration = Binder.get(environment)
                .bind("data-bridge.configuration", Bindable.of(DataBridgeConfiguration.class))
                .orElseThrow(() -> new DataBridgeException("Unable to create dataBridgeConfiguration: missing config"));
        fileMetadataConfigurations = dataBridgeConfiguration.getFiles();
        this.sftpSessionFactory = sftpSessionFactory;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        fileMetadataConfigurations.forEach(
                fileMetadataConfiguration -> {
                    GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                    beanDefinition.setBeanClass(IntegrationFlow.class);
                    beanDefinition.setInstanceSupplier(() -> {
                        try {
                            return sftpInboundFlow(sftpSessionFactory, fileMetadataConfiguration);
                        } catch (IOException e) {
                            throw new DataBridgeException(String.format("Unable to create inboundChannelAdapter for file type %s", fileMetadataConfiguration.fileType()), e);
                        }
                    });
                    registry.registerBeanDefinition(String.format("sftpInboundChannel%sBean", fileMetadataConfiguration.fileType()), beanDefinition);
                });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        /*unused*/
    }

    private IntegrationFlow sftpInboundFlow(SessionFactory<SftpClient.DirEntry> sftpSessionFactory, FileMetadataConfiguration fileMetadataConfiguration)  throws IOException {
        return IntegrationFlow
                .from(Sftp.inboundAdapter(sftpSessionFactory)
                                .deleteRemoteFiles(false)
                                .preserveTimestamp(true)
                                .filter(SftpInboundFileFilter.sftpInboundFileFilter(fileMetadataConfiguration))
                                .remoteDirectory(sftpConfiguration.getBaseDirectory() + fileMetadataConfiguration.sourceDirectory())
                                .localDirectory(Files.createTempDirectory(SYNCHRONIZE_TEMP_DIRECTORY_PREFIX).toFile())
                                .autoCreateLocalDirectory(true)
                                .localFilter(new FileSystemPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "")),
                        e -> e.id("sftpSourceChannel" + fileMetadataConfiguration.fileType())
                                .autoStartup(true)
                                .poller(Pollers.fixedDelay(sftpConfiguration.getPollingDelayInMs()).maxMessagesPerPoll(sftpConfiguration.getMaxMessagesPerPoll())))
                .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"sftp treatment of file \" + headers.file_name"))
                .channel("archivesChannel")
                .get();
    }

}
