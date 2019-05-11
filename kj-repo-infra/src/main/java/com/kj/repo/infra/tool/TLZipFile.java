package com.kj.repo.infra.tool;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author kuojian21
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

    /**
     * @author kuojian21
     */
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t) throws Exception;
    }

    /**
     * @author kuojian21
     */
    @FunctionalInterface
    public interface BiConsumer<T, U> {
        void accept(T t, U u) throws Exception;
    }

}
