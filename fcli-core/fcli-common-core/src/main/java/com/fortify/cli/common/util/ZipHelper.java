/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;

import lombok.SneakyThrows;

public class ZipHelper {
    /**
     * Opens an existing zip file as a {@link FileSystem}. Callers are responsible for
     * closing the returned file system (for example via try-with-resources).
     *
     * @param zipFile path to an existing zip file
     * @return a zip file system for reading (and optionally writing) zip entries
     * @throws FcliTechnicalException if the zip file cannot be opened
     */
    public static final FileSystem openZipFileSystem(Path zipFile) {
        try {
            return FileSystems.newFileSystem(zipFile, (ClassLoader)null);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error opening zip file " + zipFile, e);
        }
    }

    /**
     * Creates a new zip file as a {@link FileSystem}. Parent directories are created
     * if needed. If {@code zipFile} already exists, it is deleted first. Callers are
     * responsible for closing the returned file system (for example via try-with-resources).
     *
     * @param zipFile path of the zip file to create
     * @return a zip file system opened with {@code create=true}
     * @throws FcliTechnicalException if the zip file cannot be created or opened
     */
    public static final FileSystem createZipFileSystem(Path zipFile) {
        try {
            var parent = zipFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.deleteIfExists(zipFile);
            return FileSystems.newFileSystem(zipFile, Map.of("create", "true"));
        } catch (IOException e) {
            throw new FcliTechnicalException("Error creating zip file " + zipFile, e);
        }
    }

    /**
     * Helper method to process individual zip entries from a zip file
     * loaded from the given zipFileInputStream, calling the 
     * {@link IZipEntryProcessor#process(ZipInputStream, ZipEntry)}
     * method for each zip entry. Processing is terminated if the
     * processor returns {@link Break#TRUE}. 
     * 
     * @param zipFileInputStream The {@link InputStream} to be read as a zip file.
     * @param processor The {@link IZipEntryProcessor} used to process each zip entry.
     * @return {@link Break#TRUE} if the processor returned {@link Break#TRUE} for any
     *         entry, {@link Break#FALSE} otherwise.
     */
    public static final Break processZipEntries(InputStream zipFileInputStream, IZipEntryProcessor processor) {
        if ( zipFileInputStream!=null ) {
            try ( ZipInputStream zis = new ZipInputStream(zipFileInputStream) ) {
                ZipEntry entry;
                while ( (entry = zis.getNextEntry())!=null ) {
                    if ( processor.process(zis, entry).doBreak() ) { return Break.TRUE; }
                }
            } catch (IOException e) {
                throw new FcliSimpleException("Error loading zip entry", e);
            }
        }
        return Break.FALSE;
    }
    
    /**
     * Same as {@link #processZipEntries(InputStream, IZipEntryProcessor)},
     * but taking an {@link InputStream} {@link Supplier} instead of plain
     * {@link InputStream}.
     * @param zipFileInputStreamSupplier {@link Supplier} of the {@link InputStream} to be read as a zip file.
     * @param processor The {@link IZipEntryProcessor} used to process each zip entry.
     * @return {@link Break#TRUE} if the processor returned {@link Break#TRUE} for any
     *         entry, {@link Break#FALSE} otherwise.
     */
    @SneakyThrows
    public static final Break processZipEntries(Supplier<InputStream> zipFileInputStreamSupplier, IZipEntryProcessor processor) {
        try (var is = zipFileInputStreamSupplier.get()) {
            return processZipEntries(is, processor);
        }
    }
    
    /**
     * Helper method to process individual zip entries from a zip file
     * loaded from the given zipFileInputStream, calling the 
     * {@link IZipEntryWithContextProcessor#process(ZipInputStream, ZipEntry, Object)}
     * method for each zip entry, also passing the arbitrary context object
     * passed to this method. Processing is terminated if the processor 
     * returns {@link Break#TRUE}. 
     * 
     * @param <C> Type of context object
     * @param zipFileInputStream The {@link InputStream} to be read as a zip file.
     * @param processor The {@link IZipEntryWithContextProcessor} used to process each zip entry.
     * @param context Arbitrary context object to be passed to the {@link IZipEntryWithContextProcessor#process(ZipInputStream, ZipEntry, Object)} method.
     * @return {@link Break#TRUE} if the processor returned {@link Break#TRUE} for any
     *         entry, {@link Break#FALSE} otherwise.
     */
    public static final <C> Break processZipEntries(InputStream zipFileInputStream, IZipEntryWithContextProcessor<C> processor, C context) {
        return processZipEntries(zipFileInputStream, (zis,ze)->processor.process(zis, ze, context));
    }
    
    /**
     * Same as {@link #processZipEntries(InputStream, IZipEntryWithContextProcessor, Object)}
     * but taking an {@link InputStream} {@link Supplier} instead of plain
     * {@link InputStream}.
     * 
     * @param <C> Type of context object
     * @param zipFileInputStream {@link Supplier} of the {@link InputStream} to be read as a zip file.
     * @param processor The {@link IZipEntryWithContextProcessor} used to process each zip entry.
     * @param context Arbitrary context object to be passed to the {@link IZipEntryWithContextProcessor#process(ZipInputStream, ZipEntry, Object)} method.
     * @return {@link Break#TRUE} if the processor returned {@link Break#TRUE} for any
     *         entry, {@link Break#FALSE} otherwise.
     */
    @SneakyThrows
    public static final <C> Break processZipEntries(Supplier<InputStream> zipFileInputStreamSupplier, IZipEntryWithContextProcessor<C> processor, C context) {
        try (var is = zipFileInputStreamSupplier.get()) {
            return processZipEntries(is, processor, context);
        }
    }

    @FunctionalInterface
    public static interface IZipEntryWithContextProcessor<C> {
        Break process(ZipInputStream zis, ZipEntry entry, C context);
    }
    
    @FunctionalInterface
    public static interface IZipEntryProcessor {
        Break process(ZipInputStream zis, ZipEntry entry);
    }
}
