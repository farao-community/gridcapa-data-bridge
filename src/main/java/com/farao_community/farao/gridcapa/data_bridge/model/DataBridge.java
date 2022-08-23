/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataBridge {
    String zoneId;
    String targetProcess;
    String fileType;
    String fileRegex;
    String timeValidity;
    List<String> remoteFileRegex;
    String ftpDirectory;
    String minioDirectory;

    public String getBridgeIdentifiant() {
        return targetProcess + "_" + fileType;
    }

}
