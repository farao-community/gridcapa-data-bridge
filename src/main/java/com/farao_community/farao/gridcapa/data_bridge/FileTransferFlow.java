/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.farao_community.farao.gridcapa.data_bridge.configuration.DataBridgeConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileMetadataConfiguration;
import com.farao_community.farao.gridcapa.data_bridge.configuration.FileType;
import com.farao_community.farao.gridcapa.data_bridge.exception.DocumentIdExtractionException;
import com.farao_community.farao.gridcapa.data_bridge.sax.DocumentIdReader;
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
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
public class FileTransferFlow {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final DataBridgeConfiguration dataBridgeConfiguration;
    private final SAXParser saxParser;

    public FileTransferFlow(DataBridgeConfiguration dataBridgeConfiguration, SAXParser saxParser) {
        this.dataBridgeConfiguration = dataBridgeConfiguration;
        this.saxParser = saxParser;
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
    public MessageChannel documentIdErrorChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow unzipArchivesIntegrationFlow() {
        return IntegrationFlow.from("archivesChannel")
                .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"Pre-treatment of file \" + headers.file_name"))
                .<File, Boolean>route(file -> {
                            final FileMetadataConfiguration fileMetadataConfiguration = dataBridgeConfiguration.getFileConfigurationFromRemoteName(file.getName());
                            return isZip(file) && fileMetadataConfiguration.doUnzip();
                        }, m -> m
                                .subFlowMapping(false, flow -> flow
                                        .transform(Message.class, this::addHeaders)
                                        .channel("filesChannel")
                                )
                                .subFlowMapping(true, flow -> flow
                                        .transform(new UnZipTransformer())
                                        .split(new UnZipResultSplitter())
                                        .transform(Message.class, this::addHeaders)
                                        .channel("filesChannel")
                                )
                )
                .get();
    }

    @Bean
    public IntegrationFlow handleDocumentIdError() {
        return IntegrationFlow.from("documentIdErrorChannel")
                .log(LoggingHandler.Level.ERROR, PARSER.parseExpression("\"Error occurred during treatment of file \" + payload.failedMessage.headers.file_name + \" : DocumentId could not be found\n  Caused by: \" + payload.failedMessage.headers.cause"))
                .log(LoggingHandler.Level.INFO, PARSER.parseExpression("\"File \" + payload.failedMessage.headers.file_name + \" will be ignored\""))
                .channel("nullChannel") // default channel in Spring Integration to terminate a flow without returning a value
                .get();
    }

    private boolean isZip(File file) {
        return ZipFileDetector.isZip(file);
    }

    Message<File> addHeaders(Message<File> message) {
        final String filename = (String) message.getHeaders().get("file_name");
        final String fileSinkDirectory = dataBridgeConfiguration.getFileConfigurationFromName(filename).sinkDirectory();

        final File inputFile = message.getPayload();
        final FileMetadataConfiguration fileMetadataConfiguration = dataBridgeConfiguration.getFileConfigurationFromRemoteName(inputFile.getName());
        final String documentId;
        try {
            documentId = getDocumentId(inputFile, fileMetadataConfiguration.fileType());
        } catch (DocumentIdExtractionException e) {
            return MessageBuilder.fromMessage(message)
                    .setErrorChannelName("documentIdErrorChannel")
                    .setHeader("cause", e)
                    .build();
        }

        return MessageBuilder.fromMessage(message)
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_FILE_NAME_METADATA_KEY, filename)
                .setHeader("file_sink", fileSinkDirectory)
                .setHeader(MinioAdapterConstants.DEFAULT_GRIDCAPA_DOCUMENT_ID_METADATA_KEY, documentId)
                .build();
    }

    private String getDocumentId(File inputFile, FileType fileType) throws DocumentIdExtractionException {
        return switch (fileType) {
            case CGM, DCCGM -> getDocumentIdFromZip(inputFile.toPath().toString());
            case CBCORA, GLSK, REFPROG -> getDocumentIdFromXml(inputFile);
            default -> null;
        };
    }

    private String getDocumentIdFromZip(String inputFilePath) throws DocumentIdExtractionException {
        try (ZipFile zipFile = new ZipFile(inputFilePath)) {
            ZipEntry cgmHeaderEntry = zipFile.getEntry("CGM_XML_HEADER.xml");
            if (cgmHeaderEntry != null) {
                try (InputStream inputStream = zipFile.getInputStream(cgmHeaderEntry)) {
                    return getDocumentIdFromXml(inputStream);
                }
            } else {
                throw new DocumentIdExtractionException("File 'CGM_XML_HEADER.xml' not found in archive");
            }
        } catch (IOException e) {
            throw new DocumentIdExtractionException(e);
        }
    }

    private String getDocumentIdFromXml(File file) throws DocumentIdExtractionException {
        try {
            DocumentIdReader handler = new DocumentIdReader();
            saxParser.parse(file, handler);
            String documentId = handler.getDocumentId();
            if (documentId == null) {
                throw new DocumentIdExtractionException("Element representing DocumentId not found in file content");
            }
            return documentId;
        } catch (SAXException | IOException e) {
            throw new DocumentIdExtractionException(e);
        }
    }

    private String getDocumentIdFromXml(InputStream inputStream) throws DocumentIdExtractionException {
        try {
            DocumentIdReader handler = new DocumentIdReader();
            saxParser.parse(inputStream, handler);
            String documentId = handler.getDocumentId();
            if (documentId == null) {
                throw new DocumentIdExtractionException("Element representing DocumentId not found in file content");
            }
            return documentId;
        } catch (SAXException | IOException e) {
            throw new DocumentIdExtractionException(e);
        }
    }
}
