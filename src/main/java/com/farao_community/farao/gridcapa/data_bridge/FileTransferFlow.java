/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.gridcapa.data_bridge.configuration.DataBridgeConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.utils.ZipFileDetector;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
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

    private final DataBridgeConfiguration dataBridgeConfiguration;

    public FileTransferFlow(DataBridgeConfiguration dataBridgeConfiguration) {
        this.dataBridgeConfiguration = dataBridgeConfiguration;
    }

    @Bean
    public MessageChannel archivesChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel filesChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow unzipArchivesIntegrationFlow() {
        return IntegrationFlow.from("archivesChannel")
               .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"Pre-treatment of file \" + headers.file_name"))

                .<File, Boolean>route(file -> {
                        String fileName = PARSER.parseExpression("headers.file_name").getValue(String.class);
                        final FileMetadataConfiguration fileMetadataConfiguration = dataBridgeConfiguration.getFileConfiguration(fileName);
                        return isZip(file) && fileMetadataConfiguration.doUnzip();
                       }, m -> m
                        .subFlowMapping(false, flow -> flow
                                .transform(Message.class, this::addFileNameHeader)
                                .channel("filesChannel")
                        )
                        .subFlowMapping(true, flow -> flow
                                .transform(new UnZipTransformer())
                                .split(new UnZipResultSplitter())
//                                .filter(Message.class, msg -> isFormatOk((String) msg.getHeaders().get("file_name")))
                                .transform(Message.class, this::addFileNameHeader)
                                .channel("filesChannel")
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

//    private boolean isFormatOk(String filename) {
//        return filename.matches(fileNameRegex);
//    }

}
