/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public final class ZipFileDetector {

    private ZipFileDetector() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Checks if a file is a ZIP archive.
     *
     * Actual check is based on file signature, as expressed in following link
     * https://en.wikipedia.org/wiki/List_of_file_signatures
     *
     * @param file file to check
     * @return true if file is a zip archive, false otherwise
     */
    public static boolean isZip(File file) {
        int fileSignature;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            fileSignature = raf.readInt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
    }
}
