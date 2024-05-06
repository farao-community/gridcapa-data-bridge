/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sources;

import com.farao_community.farao.gridcapa.data_bridge.configuration.DataBridgeConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.utils.DataBridgeFtpInboundFileSynchronizer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.SimpleMetadataStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel  at rte-france.com>}
 */
public class FtpDynamicBeanDefinitionRegistrar implements BeanDefinitionRegistryPostProcessor {

    private static final String SYNCHRONIZE_TEMP_DIRECTORY_PREFIX = "gridcapa-data-bridge";
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final List<FileMetadataConfiguration> fileMetadataConfigurations;

    public FtpDynamicBeanDefinitionRegistrar(DataBridgeConfiguration dataBridgeConfiguration) {
        fileMetadataConfigurations = dataBridgeConfiguration.getFiles();
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        fileMetadataConfigurations.forEach(
                fileType -> {
                    GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
//                    beanDefinition.setBeanClass(BeanClass.class);
//                    beanDefinition.setInstanceSupplier(() -> new BeanClass(beanName));
//                    beanDefinition.addMetadataAttribute();
                    registry.registerBeanDefinition(fileType.fileType(), beanDefinition);
                });
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException { }

    @Bean
    @InboundChannelAdapter(channel = "ftpSourceChannel", poller = @Poller(fixedDelay = "${data-bridge.sources.ftp.polling-delay-in-ms}", maxMessagesPerPoll = "${data-bridge.sources.ftp.max-messages-per-poll}"))
    public MessageSource<File> ftpMessageSource() throws IOException {
        FtpInboundFileSynchronizingMessageSource source =
                new FtpInboundFileSynchronizingMessageSource(DataBridgeFtpInboundFileSynchronizer.ftpInboundFileSynchronizer());
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
