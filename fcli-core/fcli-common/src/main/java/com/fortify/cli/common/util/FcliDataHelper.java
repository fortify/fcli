/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.crypto.helper.EncryptionHelper;
import com.fortify.cli.common.json.JsonHelper;

public class FcliDataHelper {
    private static final String ENVNAME_FORTIFY_DATA_DIR     = "FORTIFY_DATA_DIR";
    private static final String ENVNAME_FCLI_DATA_DIR        = "FCLI_DATA_DIR";
    private static final String ENVNAME_FCLI_CONFIG_DIR      = "FCLI_CONFIG_DIR";
    private static final String ENVNAME_FCLI_STATE_DIR       = "FCLI_STATE_DIR";
    private static final String DEFAULT_FORTIFY_DIR_NAME     = ".fortify";
    private static final String DEFAULT_FCLI_DIR_NAME        = "fcli";
    private static final String DEFAULT_FCLI_CONFIG_DIR_NAME = "config";
    private static final String DEFAULT_FCLI_STATE_DIR_NAME  = "state";
    private static final Logger LOG = LoggerFactory.getLogger(FcliDataHelper.class);
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    
    public static final Path getFortifyHomePath() {
        String fortifyData = EnvHelper.env(ENVNAME_FORTIFY_DATA_DIR);
        return StringUtils.isNotBlank(fortifyData) 
                ? Path.of(fortifyData).toAbsolutePath()
                : Path.of(System.getProperty("user.home"), DEFAULT_FORTIFY_DIR_NAME).toAbsolutePath();
    }

    public static final Path getFcliHomePath() {
        String fcliData = EnvHelper.env(ENVNAME_FCLI_DATA_DIR);
        return StringUtils.isNotBlank(fcliData) 
                ? Path.of(fcliData).toAbsolutePath()
                : getFortifyHomePath().resolve(DEFAULT_FCLI_DIR_NAME).toAbsolutePath();
    }
    
    public static final Path getFcliConfigPath() {
        String fcliConfig = EnvHelper.env(ENVNAME_FCLI_CONFIG_DIR);
        return StringUtils.isNotBlank(fcliConfig) 
                ? Path.of(fcliConfig).toAbsolutePath()
                : getFcliHomePath().resolve(DEFAULT_FCLI_CONFIG_DIR_NAME).toAbsolutePath();
    }
    
    public static final Path getFcliStatePath() {
        String fcliState = EnvHelper.env(ENVNAME_FCLI_STATE_DIR);
        return StringUtils.isNotBlank(fcliState) 
                ? Path.of(fcliState).toAbsolutePath()
                : getFcliHomePath().resolve(DEFAULT_FCLI_STATE_DIR_NAME).toAbsolutePath();
    }
    
    public static final void saveSecuredFile(Path relativePath, Object contents, boolean failOnError) {
        if ( contents == null ) {
            throwOrLog("Contents may not be null", null, failOnError);
        }
        try {
            String stringContents = contents instanceof String 
                    ? (String)contents
                    : objectMapper.writeValueAsString(contents);
            saveFile(relativePath, EncryptionHelper.encrypt(stringContents), failOnError);
        } catch (JsonProcessingException e) {
            throwOrLog("Error serializing contents as String for class "+contents.getClass().getName(), e, failOnError);
        }
    }
    
    public static final String readSecuredFile(Path relativePath, boolean failOnError) {
        return readSecuredFile(relativePath, String.class, failOnError);
    }
    
    
    @SuppressWarnings("unchecked")
    public static final <T> T readSecuredFile(Path relativePath, Class<T> returnType, boolean failOnError) {
        String contents = EncryptionHelper.decrypt(readFile(relativePath, failOnError));
        return String.class.isAssignableFrom(returnType) 
                ? (T)contents 
                : JsonHelper.jsonStringToValue(readSecuredFile(relativePath, failOnError), returnType);
    }
    
    public static final void saveFile(Path relativePath, Object contents, boolean failOnError) {
        if ( contents == null ) {
            throwOrLog("Contents may not be null", null, failOnError);
        }
        try {
            String stringContents = contents instanceof String 
                    ? (String)contents
                    : objectMapper.writeValueAsString(contents);
            final Path filePath = resolveFcliHomePath(relativePath);
            final Path parentDir = filePath.getParent();
            if (!Files.exists(parentDir)) {
                try {
                    Files.createDirectories(parentDir);
                } catch ( IOException e ) {
                    throwOrLog("Error creating parent directories for "+filePath, e, failOnError);
                }
            }
            writeFileWithOwnerOnlyPermissions(filePath, stringContents, failOnError);
        } catch (JsonProcessingException e ) {
            throwOrLog("Error serializing contents as String for class "+contents.getClass().getName(), e, failOnError);
        }
    }

    private static void writeFileWithOwnerOnlyPermissions(final Path filePath, final String contents, boolean failOnError) {
        try (var fos = new FileOutputStream(filePath.toString()); var osw = new OutputStreamWriter(fos, "UTF-8"); BufferedWriter  writer = new BufferedWriter(osw); ){
            writer.write("");
            if ( FileSystems.getDefault().supportedFileAttributeViews().contains("posix") ) {
                Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rw-------"));
            } else {
                File file = filePath.toFile();
                file.setExecutable(false, false);
                file.setReadable(true, true);
                file.setWritable(true, true);
            }
           writer.write(contents);
           writer.close();
        } catch ( IOException e ) {
            throwOrLog("Error writing file "+filePath, e, failOnError);
        }
    }
    
    public static final String readFile(Path relativePath, boolean failOnError) {
        return readFile(relativePath, String.class, failOnError);
    }
    
    @SuppressWarnings("unchecked")
    public static final <R> R readFile(Path relativePath, Class<R> returnType, boolean failOnError) {
        final Path filePath = resolveFcliHomePath(relativePath);
        try {
            String contents = Files.readString(filePath, StandardCharsets.UTF_8);
            return String.class.isAssignableFrom(returnType)
                    ? (R)contents 
                    : JsonHelper.jsonStringToValue(contents, returnType);
        } catch ( IOException e ) {
            throwOrLog("Error reading file "+filePath, e, failOnError);
            return null;
        }
    }
    
    public static final boolean isReadable(Path relativePath) {
        final Path filePath = resolveFcliHomePath(relativePath);
        return Files.isReadable(filePath);
    }
    
    public static final Stream<Path> listFilesInDir(Path relativePath, boolean failOnError) {
        Stream<Path> stream = listDir(relativePath, failOnError);
        return stream ==null ? null : stream.filter(Files::isRegularFile);
    }
    
    public static final Stream<Path> listDirsInDir(Path relativePath, boolean failOnError) {
        Stream<Path> stream = listDir(relativePath, failOnError);
        return stream==null ? null : stream.filter(Files::isDirectory);
    }
    
    public static final Stream<Path> listDir(Path relativePath, boolean failOnError) {
        final Path dirPath = resolveFcliHomePath(relativePath);
        try {
            return Files.list(dirPath);
        } catch ( IOException e ) {
            throwOrLog("Error getting directory listing for "+dirPath, e, failOnError);
            return null;
        }
    }
    
    public static final void deleteFile(Path relativePath, boolean failOnError) {
        final Path filePath = resolveFcliHomePath(relativePath);
        try {
            Files.deleteIfExists(filePath);
        } catch ( IOException e ) {
            throwOrLog("Error deleting file "+filePath, e, failOnError);
        }
    }
    
    public static final void deleteDir(Path relativePath, boolean failOnError) {
        final Path filePath = resolveFcliHomePath(relativePath);
        try {
            Files.walk(filePath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch ( IOException e ) {
            throwOrLog("Error recursively deleting directory "+filePath, e, failOnError);
        }
    }

    public static final boolean exists(Path relativePath) {
        final Path filePath = resolveFcliHomePath(relativePath);
        return Files.exists(filePath);
    }
    
    public static Path resolveFcliHomePath(Path relativePath) {
        if ( relativePath.isAbsolute() && !relativePath.toAbsolutePath().startsWith(getFcliHomePath()) ) {
            throw new IllegalArgumentException(String.format("Path %s is not within fcli home directory", relativePath));
        }
        return getFcliHomePath().resolve(relativePath);
    }
    
    private static final void throwOrLog(String msg, Exception e, boolean failOnError) {
        if ( failOnError ) {
            throw new RuntimeException(msg, e);
        } else {
            LOG.info(msg, e);
        }
    }
}
