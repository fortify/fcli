/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import lombok.SneakyThrows;

// TODO For now, methods provided in this class are only used by the tools module,
//      but potentially some methods or the full class could be moved to the common module.
public final class FileUtils {
    private FileUtils() {}
    
    public static final InputStream getResourceInputStream(String resourcePath) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    }
    
    @SneakyThrows
    public static final String readResourceAsString(String resourcePath, Charset charset) {
        return new String(readResourceAsBytes(resourcePath), charset);
    }
    
    @SneakyThrows
    public static final byte[] readResourceAsBytes(String resourcePath) {
        try ( InputStream in = getResourceInputStream(resourcePath) ) {
            return in.readAllBytes();
        }
    }
    
    public static final void copyResource(String resourcePath, Path destinationFilePath, CopyOption... options) {
        var parent = destinationFilePath.getParent();
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating directory %s", parent), e);
        }
        try ( InputStream in = getResourceInputStream(resourcePath) ) {
            Files.copy( in, destinationFilePath, options);
        } catch ( IOException e ) {
            throw new RuntimeException(String.format("Error copying resource %s to %s", resourcePath, destinationFilePath), e);
        }
    }
    
    public static final void copyResourceToDir(String resourcePath, Path destinationPath, CopyOption... options) {
        String fileName = Paths.get(resourcePath).getFileName().toString();
        copyResource(resourcePath, destinationPath.resolve(fileName), options);
    }
    
    @SneakyThrows
    public static final void moveFiles(Path sourcePath, Path targetPath, String regex) {
        Files.createDirectories(targetPath);
        try ( var ls = Files.list(sourcePath) ) {
            ls.map(Path::toFile)
                .map(File::getName)
                .filter(name->name.matches(regex))
                .forEach(name->move(sourcePath.resolve(name), targetPath.resolve(name)));
        }
    }
    
    public static final void move(Path source, Path target) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error moving %s to %s", source, target), e);
        }
    }
    
    @SneakyThrows
    public static final void extractZip(File zipFile, Path targetDir) {
        try (FileInputStream fis = new FileInputStream(zipFile); ZipInputStream zipIn = new ZipInputStream(fis)) {
            for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
                Path resolvedPath = targetDir.resolve(ze.getName()).normalize();
                if (!resolvedPath.startsWith(targetDir.normalize())) {
                    // see: https://snyk.io/research/zip-slip-vulnerability
                    throw new RuntimeException("Entry with an illegal path: " + ze.getName());
                }
                if (ze.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zipIn, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
    
    @SneakyThrows
    public static final void extractTarGZ(File tgzFile, Path targetDir) {
        try (InputStream source = Files.newInputStream(tgzFile.toPath());
                GZIPInputStream gzip = new GZIPInputStream(source);
                TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {

            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                Path extractTo = targetDir.resolve(entry.getName());
                if(entry.isDirectory()) {
                    Files.createDirectories(extractTo);
                } else {
                    Files.copy(tar, extractTo, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
    
    /**
     * Recursively delete the given path. As a best practice, this method should
     * only be invoked if {@link #isDirPathInUse(Path)} returns false. The
     * deleteRecursive() method itself doesn't invoke {@link #isDirPathInUse(Path)}
     * for performance reasons, as callers may wish to explicitly check whether
     * any files are in use in order to perform some alternative action.
     * @param path
     */
    @SneakyThrows
    public static final void deleteRecursive(Path path) {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
    
    @SneakyThrows
    public static final boolean isDirPathInUse(Path path) {
        if ( isDirPathInUseByCurrentExecutable(path) ) { return true; }
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.anyMatch(FileUtils::isFilePathInUse);
        }
    }
    
    @SneakyThrows
    public static final boolean isDirPathInUseByCurrentExecutable(Path path) {
        var currentExecutablePath = Path.of(FileUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return currentExecutablePath.normalize().startsWith(path.normalize());
    }
    
    @SneakyThrows
    public static final boolean isFilePathInUse(Path path) {
        if ( path.toFile().isFile() ) {
            try ( var fc = FileChannel.open(path, StandardOpenOption.APPEND) ) {
                if ( fc.tryLock()==null ) {
                    return true;
                }
            } catch ( FileSystemException e ) {
                return true;
            }
        }
        return false;
    }
}
