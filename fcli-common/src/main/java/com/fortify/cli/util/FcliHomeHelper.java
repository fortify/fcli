package com.fortify.cli.util;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.micronaut.core.util.StringUtils;

public class FcliHomeHelper {
	private static final String FORTIFY_HOME_ENV_VAR_NAME = "FORTIFY_HOME";
	private static final String SSC_CLI_CONFIG_DIR_NAME = "fcli";

	public static final Path getFcliHomePath() {
		String fortifyHomeUri = System.getenv(FORTIFY_HOME_ENV_VAR_NAME);
		if (StringUtils.isEmpty(fortifyHomeUri)) {
			fortifyHomeUri = Path.of(System.getProperty("user.home"), ".fortify").toString();
		}
		return Path.of(fortifyHomeUri, SSC_CLI_CONFIG_DIR_NAME);
	}
	
	public static final void saveSecuredFile(Path relativePath, String contents) throws IOException {
		saveFile(relativePath, EncryptionHelper.encrypt(contents));
		// TODO Where possible, allow only owner r/w access
	}
	
	public static final String readSecuredFile(Path relativePath, boolean failIfNotReadable) throws IOException {
		return EncryptionHelper.decrypt(readFile(relativePath, failIfNotReadable));
	}
	
	public static final void saveFile(Path relativePath, String contents) throws IOException {
		// FIXME: make sure the file is not world writable
		// see FileSystem.supportedFileAttributeViews() and AttributeView
		// for possible multiplatform solution
		final Path filePath = getFcliHomePath().resolve(relativePath);
		final Path parentDir = filePath.getParent();
		if (!Files.exists(parentDir)) {
			Files.createDirectories(parentDir);
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
	
	public static final void deleteFile(Path relativePath) throws IOException {
		final Path filePath = getFcliHomePath().resolve(relativePath);
		Files.deleteIfExists(filePath);
	}
	
	public static final void deleteFilesInDir(Path relativePath, boolean recursive) throws IOException {
		listFilesInDir(relativePath, recursive).map(Path::toFile).forEach(File::delete);
	}
}
