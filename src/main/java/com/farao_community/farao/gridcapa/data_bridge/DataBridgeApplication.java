/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@SuppressWarnings("hideutilityclassconstructor")
@SpringBootApplication
public class DataBridgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataBridgeApplication.class, args);
    }
}
