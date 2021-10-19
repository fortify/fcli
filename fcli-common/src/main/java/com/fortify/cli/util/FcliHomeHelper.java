package com.fortify.cli.util;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
	
	public static final void saveEncryptedFile(Path path, String contents) throws IOException {
		saveFile(path, EncryptionHelper.encrypt(contents));
	}
	
	public static final String readEncryptedFile(Path path, boolean failIfNotReadable) throws IOException {
		return EncryptionHelper.decrypt(readFile(path, failIfNotReadable));
	}
	
	public static final void saveFile(Path path, String contents) throws IOException {
		// FIXME: make sure the file is not world writable
		// see FileSystem.supportedFileAttributeViews() and AttributeView
		// for possible multiplatform solution
		final Path filePath = getFcliHomePath().resolve(path);
		final Path parentDir = filePath.getParent();
		if (!Files.exists(parentDir)) {
			Files.createDirectories(parentDir);
		}
		Files.writeString(filePath, contents, CREATE, WRITE, TRUNCATE_EXISTING);
	}
	
	public static final String readFile(Path path, boolean failIfNotReadable) throws IOException {
		final Path filePath = getFcliHomePath().resolve(path);
		if (Files.isReadable(filePath) ) {
			return Files.readString(filePath);
		} else if ( failIfNotReadable ) {
			throw new IllegalArgumentException("File cannot be read: "+filePath.toString());
		} else {
			return null;
		}
	}
	
	public static final void deleteFile(String relativeFilePath) throws IOException {
		final Path filePath = getFcliHomePath().resolve(relativeFilePath);
		Files.deleteIfExists(filePath);
	}
}
