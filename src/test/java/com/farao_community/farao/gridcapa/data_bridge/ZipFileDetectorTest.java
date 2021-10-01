/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class ZipFileDetectorTest {

    @Test
    void isZipForZipFileWithZipLowerCaseExtensionMustBeTrue() {
        File file = new File(getClass().getResource("/archive.zip").getFile());
        assertTrue(ZipFileDetector.isZip(file));
    }

    @Test
    void isZipForZipFileWithZipUpperCaseExtensionMustBeTrue() {
        File file = new File(getClass().getResource("/archive.ZIP").getFile());
        assertTrue(ZipFileDetector.isZip(file));
    }

    @Test
    void isZipForZipFileWithCustomExtensionMustBeTrue() {
        File file = new File(getClass().getResource("/archive.custom").getFile());
        assertTrue(ZipFileDetector.isZip(file));
    }

    @Test
    void isZipForZipFileWithoutExtensionMustBeTrue() {
        File file = new File(getClass().getResource("/archive").getFile());
        assertTrue(ZipFileDetector.isZip(file));
    }

    @Test
    void isZipForNoZipFileWithCustomExtensionMustBeFalse() {
        File file = new File(getClass().getResource("/file.custom").getFile());
        assertFalse(ZipFileDetector.isZip(file));
    }

    @Test
    void isZipForNoZipFileWithZipLowerCaseExtensionMustBeFalse() {
        File file = new File(getClass().getResource("/file.zip").getFile());
        assertFalse(ZipFileDetector.isZip(file));
    }

    @Test
    void isZipForNoZipFileWithZipUpperCaseExtensionMustBeFalse() {
        File file = new File(getClass().getResource("/file.ZIP").getFile());
        assertFalse(ZipFileDetector.isZip(file));
    }

    @Test
    void isZipForNoZipFileWithoutExtensionMustBeFalse() {
        File file = new File(getClass().getResource("/file").getFile());
        assertFalse(ZipFileDetector.isZip(file));
    }
}
