/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public final class MetadataStore {

    private static PropertiesPersistingMetadataStore metadataStore;

    private MetadataStore() {
    }

    public static PropertiesPersistingMetadataStore createMetadataStore() {
        metadataStore = new PropertiesPersistingMetadataStore();
        metadataStore.setBaseDirectory(System.getProperty("java.io.tmpdir") + "/spring-integration/");
        metadataStore.afterPropertiesSet();
        return metadataStore;
    }

    public static void flushMetadataStore() {
        metadataStore.flush();
    }
}
