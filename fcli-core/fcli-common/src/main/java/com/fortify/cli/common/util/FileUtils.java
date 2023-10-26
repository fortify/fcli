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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.digest.DigestUtils;

// TODO For now, methods provided in this class are only used by the tools module,
//      but potentially some methods or the full class could be moved to the common module.
public final class FileUtils {
    private FileUtils() {}
    
    public static final void copyResource(String resourcePath, Path destinationFilePath, CopyOption... options) {
        try ( InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath) ) {
            Files.copy( in, destinationFilePath, options);
        } catch ( IOException e ) {
            throw new RuntimeException(String.format("Error copying resource %s to %s", resourcePath, destinationFilePath), e);
        }
    }
    
    public static final void copyResourceToDir(String resourcePath, Path destinationPath, CopyOption... options) {
        String fileName = Paths.get(resourcePath).getFileName().toString();
        copyResource(resourcePath, destinationPath.resolve(fileName), options);
    }
    
    public static final String getFileDigest(File file, String algorithm) {
        try {
            MessageDigest digestInstance = MessageDigest.getInstance(algorithm);
            return bytesToHex(DigestUtils.digest(digestInstance, file));
        } catch ( IOException | NoSuchAlgorithmException e ) {
            throw new RuntimeException("Error calculating file digest for file "+file.getAbsolutePath(), e);
        }
    }
    
    public static final String getDigest(String inputName, InputStream input, String algorithm) {
        try {
            MessageDigest digestInstance = MessageDigest.getInstance(algorithm);
            return bytesToHex(DigestUtils.digest(digestInstance, input));
        } catch ( IOException | NoSuchAlgorithmException e ) {
            throw new RuntimeException("Error calculating file digest for file "+inputName, e);
        }
    }
    
    private static final String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    public static final void extractZip(File zipFile, Path targetDir) throws IOException {
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
                    Files.copy(zipIn, resolvedPath);
                }
            }
        }
    }
    
    public static final void deleteRecursive(Path installPath) throws IOException {
        try (Stream<Path> walk = Files.walk(installPath)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
