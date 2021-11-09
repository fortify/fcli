package com.fortify.cli.common.home;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;

import com.fortify.cli.common.encrypt.EncryptionHelper;

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
	
	public static final void deleteFile(Path relativePath) throws IOException {
		final Path filePath = getFcliHomePath().resolve(relativePath);
		Files.deleteIfExists(filePath);
	}
	
	public static final void deleteFilesInDir(Path relativePath, boolean recursive) throws IOException {
		listFilesInDir(relativePath, recursive).map(Path::toFile).forEach(File::delete);
	}

	public static final boolean exists(Path relativePath) {
		final Path filePath = getFcliHomePath().resolve(relativePath);
		return Files.exists(filePath);
	}
}
