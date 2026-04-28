package com.report.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipPackager {

    private static final Logger log = LoggerFactory.getLogger(ZipPackager.class);

    public static String packageFiles(String zipPath, List<String> filePaths) throws IOException {
        new File(zipPath).getParentFile().mkdirs();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            for (String filePath : filePaths) {
                File file = new File(filePath);
                if (file.exists()) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }
        log.info("ZIP packaged: {} ({} files)", zipPath, filePaths.size());
        return zipPath;
    }

    public static String packageFiles(String zipPath, String... filePaths) throws IOException {
        return packageFiles(zipPath, java.util.Arrays.asList(filePaths));
    }
}
