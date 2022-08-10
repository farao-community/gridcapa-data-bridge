/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.gridcapa.data_bridge.sources.RemoteFileConfiguration;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterConstants;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.zip.splitter.UnZipResultSplitter;
import org.springframework.integration.zip.transformer.UnZipTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Component
public class FileTransferFlow {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private final ApplicationContext applicationContext;

    private RemoteFileConfiguration remoteFilesConfiguration;

    public FileTransferFlow(ApplicationContext applicationContext, RemoteFileConfiguration remoteFilesConfiguration) {
        this.remoteFilesConfiguration = remoteFilesConfiguration;
        this.applicationContext = applicationContext;
        this.remoteFilesConfiguration.getDataBridgeList().stream().forEach(bridge -> {
            MessageChannel archivesChannel = new DirectChannel();
            this.applicationContext.getAutowireCapableBeanFactory().initializeBean(archivesChannel, bridge.getBridgeIdentifiant() + "_archives_channel");
            MessageChannel filesChannel = new DirectChannel();
            this.applicationContext.getAutowireCapableBeanFactory().initializeBean(filesChannel, bridge.getBridgeIdentifiant() + "_files_channel");
            IntegrationFlow flow = unzipArchivesIntegrationFlow(
                    bridge.getBridgeIdentifiant() + "_archives_channel",
                    bridge.getBridgeIdentifiant() + "_files_channel",
                    bridge.getFileRegex());
            this.applicationContext.getAutowireCapableBeanFactory().initializeBean(flow, bridge.getBridgeIdentifiant() + "_unzip_flow");
        });
    }

    public IntegrationFlow unzipArchivesIntegrationFlow(String from, String to, String fileRegex) {
        return IntegrationFlows.from(from)
               .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"Pre-treatment of file \" + headers.file_name"))

                .<File, Boolean>route(this::isZip, m -> m
                        .subFlowMapping(false, flow -> flow
                                .transform(Message.class, this::addFileNameHeader)
                                .channel(to)

                        )
                        .subFlowMapping(true, flow -> flow
                                .transform(new UnZipTransformer())
                                .split(new UnZipResultSplitter())
                                .filter(Message.class, msg -> isFormatOk((String) msg.getHeaders().get("file_name"), fileRegex))
                                .transform(Message.class, this::addFileNameHeader)
                                .channel(to)
                        )
                )
                .get();
    }

    private boolean isZip(File file) {
        return ZipFileDetector.isZip(file);
    }

    private Message<File> addFileNameHeader(Message<File> message) {
        String filename = (String) message.getHeaders().get("file_name");
        return MessageBuilder.fromMessage(message)
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, filename)
                .build();
    }

    private boolean isFormatOk(String filename, String regex) {
        return filename.matches(regex);
    }

}
