/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
