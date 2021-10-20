/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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

    @Value("${data-bridge.file-regex}")
    private String fileNameRegex;

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
        return IntegrationFlows.from("archivesChannel")
               .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"Pre-treatment of file \" + headers.file_name"))

                .<File, Boolean>route(this::isZip, m -> m
                        .subFlowMapping(false, flow -> flow
                                .transform(Message.class, this::addFileNameHeader)
                                .channel("filesChannel")
                        )
                        .subFlowMapping(true, flow -> flow
                                .transform(new UnZipTransformer())
                                .split(new UnZipResultSplitter())
                                .filter(Message.class, msg -> isFormatOk((String) msg.getHeaders().get("file_name")))
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
                .setHeader(FileMetadataProvider.GRIDCAPA_FILE_NAME_KEY, filename)
                .build();
    }

    private boolean isFormatOk(String filename) {
        return filename.matches(fileNameRegex);
    }

}
