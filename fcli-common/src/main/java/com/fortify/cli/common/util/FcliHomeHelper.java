package com.fortify.cli.common.util;

import static java.nio.file.StandardOpenOption.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;

public class FcliHomeHelper {
    private static final String ENVNAME_FORTIFY_HOME     = "FORTIFY_HOME";
    private static final String ENVNAME_FCLI_HOME        = "FCLI_HOME";
    private static final String ENVNAME_FCLI_PIPELINE_ID = "FCLI_PIPELINE_ID";
    private static final String DEFAULT_FORTIFY_DIR_NAME = ".fortify";
    private static final String DEFAULT_FCLI_DIR_NAME    = "fcli";
    
    public static final Path getFortifyHomePath() {
        String fortifyHome = System.getenv(ENVNAME_FORTIFY_HOME);
        return StringUtils.isNotBlank(fortifyHome) 
                ? Path.of(fortifyHome)
                : Path.of(System.getProperty("user.home"), DEFAULT_FORTIFY_DIR_NAME);
    }

    public static final Path getFcliHomePath() {
        String fcliHome = System.getenv(ENVNAME_FCLI_HOME);
        Path fcliHomePath = StringUtils.isNotBlank(fcliHome) 
                ? Path.of(fcliHome) 
                : getFortifyHomePath().resolve(DEFAULT_FCLI_DIR_NAME);
        String pipelineId = System.getenv(ENVNAME_FCLI_PIPELINE_ID);
        if ( StringUtils.isNotBlank(pipelineId) ) {
            fcliHomePath = fcliHomePath.resolve(String.format("pipeline_%s", pipelineId.replaceAll("\\W+", "_")));
        }
        return fcliHomePath;
    }
    
    public static final void saveSecuredFile(Path relativePath, String contents) throws IOException {
        saveFile(relativePath, EncryptionHelper.encrypt(contents));
    }
    
    public static final String readSecuredFile(Path relativePath, boolean failIfNotReadable) throws IOException {
        return EncryptionHelper.decrypt(readFile(relativePath, failIfNotReadable));
    }
    
    public static final void saveFile(Path relativePath, String contents) throws IOException {
        final Path filePath = getFcliHomePath().resolve(relativePath);
        final Path parentDir = filePath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        writeFileWithOwnerOnlyPermissions(filePath, contents);
    }

    private static void writeFileWithOwnerOnlyPermissions(final Path filePath, final String contents) throws IOException {
        Files.writeString(filePath, "", CREATE, WRITE, TRUNCATE_EXISTING);
        if ( FileSystems.getDefault().supportedFileAttributeViews().contains("posix") ) {
            Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rw-------"));
        } else {
            File file = filePath.toFile();
            file.setExecutable(false, false);
            file.setReadable(true, true);
            file.setWritable(true, true);
        }
        Files.writeString(filePath, contents, CREATE, WRITE, TRUNCATE_EXISTING);
    }
    
    public static final String readFile(Path relativePath, boolean failIfNotReadable) throws IOException {
        final Path filePath = getFcliHomePath().resolve(relativePath);
        if (Files.isReadable(filePath) ) {
            return Files.readString(filePath);
        } else if ( failIfNotReadable ) {
            throw new IllegalArgumentException("File cannot be read: "+filePath.toString());
        } else {
            return null;
        }
    }
    
    public static final boolean isReadable(Path relativePath) {
        final Path filePath = getFcliHomePath().resolve(relativePath);
        return Files.isReadable(filePath);
    }
    
    public static final Stream<Path> listFilesInDir(Path relativePath, boolean recursive) throws IOException {
        final Path dirPath = getFcliHomePath().resolve(relativePath);
        Stream<Path> stream = recursive ? Files.walk(dirPath) : Files.list(dirPath);
        return stream.filter(Files::isRegularFile);
    }
    
    public static final Stream<Path> listDirsInDir(Path relativePath, boolean recursive) throws IOException {
        final Path dirPath = getFcliHomePath().resolve(relativePath);
        Stream<Path> stream = recursive ? Files.walk(dirPath) : Files.list(dirPath);
        return stream.filter(Files::isDirectory);
    }
    
    public static final void deleteFile(Path relativePath) throws IOException {
        final Path filePath = getFcliHomePath().resolve(relativePath);
        Files.deleteIfExists(filePath);
    }
    
    public static final void deleteDir(Path relativePath) throws IOException {
        final Path filePath = getFcliHomePath().resolve(relativePath);
        if ( Files.exists(filePath) ) {
            deleteFilesInDir(relativePath, true);
            Files.delete(filePath);
        }
    }
    
    public static final void deleteFilesInDir(Path relativePath, boolean recursive) throws IOException {
        listFilesInDir(relativePath, recursive).map(Path::toFile).forEach(File::delete);
    }

    public static final boolean exists(Path relativePath) {
        final Path filePath = getFcliHomePath().resolve(relativePath);
        return Files.exists(filePath);
    }
}
