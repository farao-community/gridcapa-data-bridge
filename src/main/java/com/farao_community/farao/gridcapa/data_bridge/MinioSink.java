/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.farao_community.farao.gridcapa.data_bridge.sources.RemoteFileConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import javax.annotation.PostConstruct;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
public class MinioSink {
    public static final String AWS_CLIENT_SIGNER_TYPE = "AWSS3V4SignerType";
    private final ApplicationContext applicationContext;
    private final RemoteFileConfiguration remoteFilesConfiguration;
    private final FileMetadataProvider fileMetadataProvider;

    @Value("${data-bridge.sinks.minio.url}")
    private String url;
    @Value("${data-bridge.sinks.minio.access-key}")
    private String accessKey;
    @Value("${data-bridge.sinks.minio.secret-key}")
    private String secretKey;
    @Value("${data-bridge.sinks.minio.bucket}")
    private String bucket;

    public MinioSink(ApplicationContext applicationContext, RemoteFileConfiguration remoteFilesConfiguration, FileMetadataProvider fileMetadataProvider) {
        this.applicationContext = applicationContext;
        this.remoteFilesConfiguration = remoteFilesConfiguration;
        this.fileMetadataProvider = fileMetadataProvider;
    }

    @PostConstruct
    public void createBean() {
        this.remoteFilesConfiguration.getDataBridgeList().stream().forEach(bridge -> {
            MessageHandler mh = s3MessageHandler(this.fileMetadataProvider, bridge.getMinioDirectory());
            DirectChannel chan = (DirectChannel) this.applicationContext.getBean(bridge.getBridgeIdentifiant() + "_files_channel");
            chan.subscribe(mh);
            this.applicationContext.getAutowireCapableBeanFactory().initializeBean(mh, bridge.getBridgeIdentifiant() + "_minio");
        });
    }

    @Bean
    public MessageChannel minioSinkChannel() {
        return new DirectChannel();
    }

    //@Bean
    //@ServiceActivator(inputChannel = "filesChannel")
    public MessageHandler s3MessageHandler(FileMetadataProvider fileMetadataProvider, String baseDirectory) {
        S3MessageHandler s3MessageHandler = new S3MessageHandler(amazonS3(), bucket);
        Expression keyExpression = new SpelExpressionParser().parseExpression("'" + baseDirectory + "/' + headers.file_name");
        s3MessageHandler.setKeyExpression(keyExpression);
        s3MessageHandler.setUploadMetadataProvider((objectMetadata, message) -> fileMetadataProvider.populateMetadata(message, objectMetadata.getUserMetadata()));
        return s3MessageHandler;
    }

    private AmazonS3 amazonS3() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride(AWS_CLIENT_SIGNER_TYPE);

        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, Regions.US_EAST_1.name()))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}
