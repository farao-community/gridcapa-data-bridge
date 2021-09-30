/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.gridcapa.data_bridge;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
class ZipFileDetectorTest {

    @Test
    void isZipForZipFileMustBeTrue() {
        File zipFileWithZipLowerCaseExtension = new File(getClass().getResource("/archive.zip").getFile());
        File zipFileWithZipUpperCaseExtension = new File(getClass().getResource("/archive.ZIP").getFile());
        File zipFileWithCustomExtension = new File(getClass().getResource("/archive.custom").getFile());
        File zipFileWithoutExtension = new File(getClass().getResource("/archive").getFile());

        assertTrue(ZipFileDetector.isZip(zipFileWithZipLowerCaseExtension));
        assertTrue(ZipFileDetector.isZip(zipFileWithZipUpperCaseExtension));
        assertTrue(ZipFileDetector.isZip(zipFileWithCustomExtension));
        assertTrue(ZipFileDetector.isZip(zipFileWithoutExtension));
    }

    @Test
    void isZipForNoZipFileMustBeFalse() {
        File noZipFileWithCustomExtension = new File(getClass().getResource("/file.custom").getFile());
        File noZipFileWithZipLowerCaseExtension = new File(getClass().getResource("/file.zip").getFile());
        File noZipFileWithZipUpperCaseExtension = new File(getClass().getResource("/file.ZIP").getFile());
        File noZipFileWithoutExtension = new File(getClass().getResource("/file").getFile());

        assertFalse(ZipFileDetector.isZip(noZipFileWithCustomExtension));
        assertFalse(ZipFileDetector.isZip(noZipFileWithZipLowerCaseExtension));
        assertFalse(ZipFileDetector.isZip(noZipFileWithZipUpperCaseExtension));
        assertFalse(ZipFileDetector.isZip(noZipFileWithoutExtension));
    }
}
