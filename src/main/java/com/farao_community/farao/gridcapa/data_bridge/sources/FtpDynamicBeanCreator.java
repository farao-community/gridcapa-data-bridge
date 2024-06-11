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
import com.farao_community.farao.gridcapa.data_bridge.configuration.FtpConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.utils.FtpInboundFileFilter;
import org.apache.commons.net.ftp.FTPFile;
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
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.SimpleMetadataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel  at rte-france.com>}
 */
public class FtpDynamicBeanCreator implements BeanDefinitionRegistryPostProcessor {

    private static final String SYNCHRONIZE_TEMP_DIRECTORY_PREFIX = "gridcapa-core-cc-data-bridge";
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final List<FileMetadataConfiguration> fileMetadataConfigurations;
    private final FtpConfiguration ftpConfiguration;
    private final SessionFactory<FTPFile> ftpSessionFactory;

    public FtpDynamicBeanCreator(SessionFactory<FTPFile> ftpSessionFactory, ConfigurableEnvironment environment) {
        this.ftpConfiguration = Binder.get(environment)
                .bind("data-bridge.sources.ftp", Bindable.of(FtpConfiguration.class))
                .orElseThrow(() -> new DataBridgeException("Unable to create ftpSessionFactory: missing ftp config"));
        DataBridgeConfiguration dataBridgeConfiguration = Binder.get(environment)
                .bind("data-bridge.configuration", Bindable.of(DataBridgeConfiguration.class))
                .orElseThrow(() -> new DataBridgeException("Unable to create dataBridgeConfiguration: missing config"));
        fileMetadataConfigurations = dataBridgeConfiguration.getFiles();
        this.ftpSessionFactory = ftpSessionFactory;

    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        fileMetadataConfigurations.forEach(
                fileMetadataConfiguration -> {
                    GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                    beanDefinition.setBeanClass(IntegrationFlow.class);
                    beanDefinition.setInstanceSupplier(() -> {
                        try {
                            return ftpInboundFlow(ftpSessionFactory, fileMetadataConfiguration);
                        } catch (IOException e) {
                            throw new DataBridgeException(String.format("Unable to create inboundChannelAdapter for file type %s", fileMetadataConfiguration.fileType()), e);
                        }
                    });
                    registry.registerBeanDefinition(String.format("ftpInboundChannel%sBean%s", fileMetadataConfiguration.fileType(), UUID.randomUUID()), beanDefinition);
                });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        /*unused*/
    }

    private IntegrationFlow ftpInboundFlow(SessionFactory<FTPFile> ftpSessionFactory, FileMetadataConfiguration fileMetadataConfiguration)  throws IOException {
        return IntegrationFlow
                .from(Ftp.inboundAdapter(ftpSessionFactory)
                                .deleteRemoteFiles(false)
                                .preserveTimestamp(true)
                                .filter(FtpInboundFileFilter.ftpInboundFileFilter(fileMetadataConfiguration))
                                .remoteDirectory(ftpConfiguration.getBaseDirectory() + fileMetadataConfiguration.sourceDirectory())
                                .localDirectory(Files.createTempDirectory(SYNCHRONIZE_TEMP_DIRECTORY_PREFIX).toFile())
                                .autoCreateLocalDirectory(true)
                                .localFilter(new FileSystemPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "")),
                        e -> e.id("ftpSourceChannel" + fileMetadataConfiguration.fileType() + UUID.randomUUID())
                                .autoStartup(true)
                                .poller(Pollers.fixedDelay(ftpConfiguration.getPollingDelayInMs()).maxMessagesPerPoll(ftpConfiguration.getMaxMessagesPerPoll())))
                .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"ftp treatment of file \" + headers.file_name"))
                .channel("archivesChannel")
                .get();
    }

}
