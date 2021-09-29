/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.springframework.messaging.Message;

import java.util.Map;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public interface MetadataProvider {
    void populateMetadata(Message<?> message, Map<String, String> metadata);
}
