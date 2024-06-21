/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.sax;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public class DocumentIdReader extends DefaultHandler {

    private String documentId = null;
    private StringBuilder fieldValue = null;
    private boolean readFieldValue = false;

    public String getDocumentId() {
        return documentId;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("DocumentIdentification".equalsIgnoreCase(qName)) {
            // F120, F301, F319
            documentId = attributes.getValue("v");
        } else if ("MessageID".equalsIgnoreCase(qName)) {
            // F119, F139
            readFieldValue = true;
        }
        // create the data container
        fieldValue = new StringBuilder();
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (readFieldValue) {
            documentId = fieldValue.toString();
            readFieldValue = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        fieldValue.append(new String(ch, start, length));
    }
}
