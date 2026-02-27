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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.crypto.helper.EncryptionHelper;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;

public class FcliDataHelper {
    private static final String ENVNAME_FORTIFY_DATA_DIR     = "FORTIFY_DATA_DIR";
    private static final String ENVNAME_FCLI_DATA_DIR        = "FCLI_DATA_DIR";
    private static final String ENVNAME_FCLI_CONFIG_DIR      = "FCLI_CONFIG_DIR";
    private static final String ENVNAME_FCLI_STATE_DIR       = "FCLI_STATE_DIR";
    private static final String DEFAULT_FORTIFY_DIR_NAME     = ".fortify";
    private static final String DEFAULT_FCLI_DIR_NAME        = "fcli/v3";
    private static final String DEFAULT_FCLI_CONFIG_DIR_NAME = "config";
    private static final String DEFAULT_FCLI_STATE_DIR_NAME  = "state";
    private static final Logger LOG = LoggerFactory.getLogger(FcliDataHelper.class);
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    
    public static final Path getFortifyHomePath() {
        String fortifyData = EnvHelper.env(ENVNAME_FORTIFY_DATA_DIR);
        return StringUtils.isNotBlank(fortifyData) 
                ? Path.of(fortifyData).toAbsolutePath()
                : Path.of(EnvHelper.getUserHome(), DEFAULT_FORTIFY_DIR_NAME).toAbsolutePath();
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
        if (checkCondition(contents == null, "Contents may not be null", failOnError)) {
            return;
        }
        try {
            String stringContents = contents instanceof String 
                    ? (String)contents
                    : objectMapper.writeValueAsString(contents);
            saveFile(relativePath, EncryptionHelper.encrypt(stringContents), failOnError);
        } catch (JsonProcessingException e) {
            throwOrLogException("Error serializing contents as String for class "+contents.getClass().getName(), e, failOnError);
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
        if (checkCondition(contents == null, "Contents may not be null", failOnError)) {
            return;
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
                    throwOrLogException("Error creating parent directories for "+filePath, e, failOnError);
                }
            }
            writeFileWithOwnerOnlyPermissions(filePath, stringContents, failOnError);
        } catch (JsonProcessingException e ) {
            throwOrLogException("Error serializing contents as String for class "+contents.getClass().getName(), e, failOnError);
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
            throwOrLogException("Error writing file "+filePath, e, failOnError);
        }
    }
    
    public static final String readFile(Path relativePath, boolean failOnError) {
        return readFile(relativePath, String.class, failOnError);
    }
    
    @SuppressWarnings("unchecked")
    public static final <R> R readFile(Path relativePath, Class<R> returnType, boolean failOnError) {
        final Path filePath = resolveFcliHomePath(relativePath);
        
        // Check expected conditions before attempting IO
        if (checkCondition(!Files.exists(filePath), "File does not exist: "+filePath, failOnError)) {
            return null;
        }
        if (checkCondition(!Files.isRegularFile(filePath), "Path exists but is not a regular file: "+filePath, failOnError)) {
            return null;
        }
        if (checkCondition(!Files.isReadable(filePath), "File is not readable: "+filePath, failOnError)) {
            return null;
        }
        
        // Attempt read - any exception here is unexpected
        try {
            String contents = Files.readString(filePath, StandardCharsets.UTF_8);
            return String.class.isAssignableFrom(returnType)
                    ? (R)contents 
                    : JsonHelper.jsonStringToValue(contents, returnType);
        } catch ( IOException e ) {
            throwOrLogException("Error reading file "+filePath, e, failOnError);
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
        
        // Check expected conditions before attempting IO
        if (checkCondition(!Files.exists(dirPath), "Directory does not exist: "+dirPath, failOnError)) {
            return null;
        }
        if (checkCondition(!Files.isDirectory(dirPath), "Path exists but is not a directory: "+dirPath, failOnError)) {
            return null;
        }
        
        try {
            return Files.list(dirPath);
        } catch ( IOException e ) {
            throwOrLogException("Error getting directory listing for "+dirPath, e, failOnError);
            return null;
        }
    }
    
    public static final void deleteFile(Path relativePath, boolean failOnError) {
        final Path filePath = resolveFcliHomePath(relativePath);
        
        // Verify it's a file if it exists
        if (Files.exists(filePath) && checkCondition(!Files.isRegularFile(filePath), 
                "Cannot delete - path exists but is not a regular file: "+filePath, failOnError)) {
            return;
        }
        
        try {
            Files.deleteIfExists(filePath);
        } catch ( IOException e ) {
            throwOrLogException("Error deleting file "+filePath, e, failOnError);
        }
    }
    
    public static final void deleteDir(Path relativePath, boolean failOnError) {
        final Path dirPath = resolveFcliHomePath(relativePath);
        
        // Return early if directory doesn't exist
        if (!Files.exists(dirPath)) {
            return;
        }
        
        // Verify it's a directory
        if (checkCondition(!Files.isDirectory(dirPath), 
                "Cannot delete - path exists but is not a directory: "+dirPath, failOnError)) {
            return;
        }
        
        try {
            Files.walk(dirPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch ( IOException e ) {
            throwOrLogException("Error recursively deleting directory "+dirPath, e, failOnError);
        }
    }

    public static final boolean exists(Path relativePath) {
        final Path filePath = resolveFcliHomePath(relativePath);
        return Files.exists(filePath);
    }
    
    public static Path resolveFcliHomePath(Path relativePath) {
        if ( relativePath.isAbsolute() && !relativePath.toAbsolutePath().startsWith(getFcliHomePath()) ) {
            throw new FcliBugException(String.format("Path %s is not within fcli home directory", relativePath));
        }
        return getFcliHomePath().resolve(relativePath);
    }
    
    /**
     * Checks an expected condition (e.g., file exists, is readable). If condition is true:
     * - When failOnError=true: throws FcliSimpleException with the given message
     * - When failOnError=false: logs message at DEBUG level
     * 
     * @param condition the condition to check; if true, error/log is triggered
     * @param msg the message to use in exception or log
     * @param failOnError whether to throw exception (true) or just log (false)
     * @return true if condition was met (error/log triggered), false otherwise
     */
    private static boolean checkCondition(boolean condition, String msg, boolean failOnError) {
        if (condition) {
            if (failOnError) {
                throw new FcliSimpleException(msg);
            } else {
                LOG.debug(msg);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Handles unexpected IO exceptions during file operations.
     * - When failOnError=true: throws FcliTechnicalException
     * - When failOnError=false: logs at WARN level with exception details
     */
    private static void throwOrLogException(String msg, Exception e, boolean failOnError) {
        if ( failOnError ) {
            throw new FcliTechnicalException(msg, e);
        } else {
            LOG.warn(msg, e);
        }
    }
}
