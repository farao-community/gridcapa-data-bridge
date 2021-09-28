/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
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
                .<File, Boolean>route(this::isZip, m -> m
                        .subFlowMapping(false, flow -> flow
                                .transform(Message.class, this::addFileNameHeader)
                                .channel("filesChannel")
                        )
                        .subFlowMapping(true, flow -> flow
                                .transform(new UnZipTransformer())
                                .split(new UnZipResultSplitter())
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
                .setHeader("gridcapa_file_name", filename)
                .build();
    }
}
