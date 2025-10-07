package com.fortify.cli.tool.definitions.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.stream.Collectors;
import com.fortify.cli.common.exception.FcliSimpleException;

public class ToolDefinitionsZipContent {
    private final Map<String, byte[]> fileContents = new HashMap<>();
    private final Map<String, FileTime> fileTimes = new HashMap<>();

    public ToolDefinitionsZipContent(Path zipPath, Set<String> requiredYamlNames) throws IOException {
        if (Files.exists(zipPath)) {
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String name = Path.of(entry.getName()).getFileName().toString();
                        try (InputStream in = zipFile.getInputStream(entry)) {
                            fileContents.put(name, in.readAllBytes());
                        }
                        fileTimes.put(name, entry.getLastModifiedTime());
                    }
                }
            }
        }
        validateRequiredYaml(requiredYamlNames);
    }

    public ToolDefinitionsZipContent(InputStream zipStream, Set<String> requiredYamlNames) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String name = Path.of(entry.getName()).getFileName().toString();
                    fileContents.put(name, zis.readAllBytes());
                    FileTime ft = entry.getLastModifiedTime();
                    fileTimes.put(name, (ft != null) ? ft : FileTime.fromMillis(0));
                }
            }
        }
        validateRequiredYaml(requiredYamlNames);
    }

    private void validateRequiredYaml(Set<String> requiredYamlNames) {
        boolean found = requiredYamlNames.stream().anyMatch(fileContents::containsKey);
        if (!found) {
            throw new FcliSimpleException("Invalid zip: missing all required YAML files: " + requiredYamlNames);
        }
    }

    public Set<String> getFileNames() {
        return fileContents.keySet();
    }

    public boolean containsFile(String name) {
        return fileContents.containsKey(name);
    }

    public byte[] getFileContent(String name) {
        return fileContents.get(name);
    }

    public FileTime getFileTime(String name) {
        return fileTimes.get(name);
    }

    public Date getFileDate(String name) {
        FileTime ft = fileTimes.get(name);
        return ft != null ? new Date(ft.toMillis()) : null;
    }
}