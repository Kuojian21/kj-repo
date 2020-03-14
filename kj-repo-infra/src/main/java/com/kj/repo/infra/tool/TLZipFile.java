package com.kj.repo.infra.tool;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.kj.repo.infra.base.function.BiConsumer;
import com.kj.repo.infra.base.function.Consumer;

/**
 * @author kj
 */
public class TLZipFile {

    public static void entry(File file, Consumer<ZipEntry> consumer) throws Exception {
        ZipFile zFile = null;
        try {
            zFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zFile.entries();
            while (entries.hasMoreElements()) {
                consumer.accept(entries.nextElement());
            }
        } finally {
            if (zFile != null) {
                zFile.close();
            }
        }
    }

    public static void entry(File file, BiConsumer<ZipEntry, InputStream> consumer) throws Exception {
        ZipFile zFile = null;
        try {
            zFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zFile.entries();
            while (entries.hasMoreElements()) {
                InputStream is = null;
                try {
                    ZipEntry entry = entries.nextElement();
                    is = zFile.getInputStream(entry);
                    consumer.accept(entry, is);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }

            }
        } finally {
            if (zFile != null) {
                zFile.close();
            }
        }
    }
}
