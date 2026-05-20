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
package com.fortify.cli.agent.extensions.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnirestHelper;

/**
 * Resolves and provides access to extension source contents.
 * Supports local zip files, local directories, and remote URLs.
 */
public final class AgentExtensionsSourceHandler implements AutoCloseable {
    public static final String DEFAULT_SOURCE_URL =
        "https://github.com/fortify/skills/releases/download/latest/fortify-agent-extensions.zip";
    private static final Logger LOG = LoggerFactory.getLogger(AgentExtensionsSourceHandler.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final Path extractedDir;
    private final boolean tempDir;

    private AgentExtensionsSourceHandler(Path extractedDir, boolean tempDir) {
        this.extractedDir = extractedDir;
        this.tempDir = tempDir;
    }

    /**
     * Resolve a source string to a source handler.
     * @param source local zip path, local directory path, or remote URL
     */
    public static AgentExtensionsSourceHandler resolve(String source) {
        if (StringUtils.isBlank(source)) {
            source = DEFAULT_SOURCE_URL;
        }
        var path = Path.of(source);
        if (Files.isDirectory(path)) {
            return new AgentExtensionsSourceHandler(path.toAbsolutePath(), false);
        }
        if (Files.isRegularFile(path)) {
            return fromZipFile(path);
        }
        if (isUrl(source)) {
            return fromUrl(source);
        }
        throw new FcliSimpleException("Source not found or unsupported: " + source);
    }

    private static boolean isUrl(String source) {
        return source.startsWith("http://") || source.startsWith("https://");
    }

    private static AgentExtensionsSourceHandler fromUrl(String url) {
        try {
            var tempZip = Files.createTempFile("fcli-extensions-", ".zip");
            try {
                UnirestHelper.download("agent", url, tempZip.toFile());
                var handler = fromZipFile(tempZip);
                return handler;
            } finally {
                Files.deleteIfExists(tempZip);
            }
        } catch (IOException e) {
            throw new FcliTechnicalException("Error downloading extensions from " + url, e);
        }
    }

    private static AgentExtensionsSourceHandler fromZipFile(Path zipPath) {
        try {
            var tempDir = Files.createTempDirectory("fcli-extensions-");
            try (var zipFile = new ZipFile(zipPath.toFile())) {
                var entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    var entryPath = tempDir.resolve(normalizePath(entry.getName()));
                    // Validate path traversal
                    if (!entryPath.normalize().startsWith(tempDir)) {
                        throw new FcliSimpleException("Zip entry contains path traversal: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            Files.copy(is, entryPath);
                        }
                    }
                }
            }
            return new AgentExtensionsSourceHandler(tempDir, true);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error extracting extensions zip: " + zipPath, e);
        }
    }

    /**
     * Normalize a zip entry path: strip leading "./" prefix.
     */
    private static String normalizePath(String path) {
        if (path.startsWith("./")) { return path.substring(2); }
        return path;
    }

    public Path getExtractedDir() {
        return extractedDir;
    }

    /**
     * Read and parse the extensions-distribution.yaml descriptor.
     */
    public AgentExtensionsDistributionDescriptor readDescriptor() {
        var descriptorPath = extractedDir.resolve("extensions-distribution.yaml");
        if (!Files.isRegularFile(descriptorPath)) {
            throw new FcliSimpleException("extensions-distribution.yaml not found in source");
        }
        try {
            return YAML_MAPPER.readValue(descriptorPath.toFile(), AgentExtensionsDistributionDescriptor.class);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading extensions-distribution.yaml", e);
        }
    }

    /**
     * Read the source version from version.txt. Returns "unknown" if not found.
     */
    public String readSourceVersion() {
        var versionPath = extractedDir.resolve("version.txt");
        if (!Files.isRegularFile(versionPath)) {
            return "unknown";
        }
        try {
            return Files.readString(versionPath).trim();
        } catch (IOException e) {
            LOG.warn("Error reading version.txt, defaulting to 'unknown'", e);
            return "unknown";
        }
    }

    /**
     * Read and parse manifest.json for signature verification.
     * @return map of normalized file path → RSA-SHA256 signature, or null if no manifest
     */
    public Map<String, String> readManifest() {
        var manifestPath = extractedDir.resolve("manifest.json");
        if (!Files.isRegularFile(manifestPath)) {
            return null;
        }
        try {
            var objectMapper = JsonHelper.getObjectMapper();
            var entries = objectMapper.readValue(manifestPath.toFile(),
                new TypeReference<java.util.List<ManifestEntry>>() {});
            var result = new HashMap<String, String>();
            for (var entry : entries) {
                result.put(normalizePath(entry.path), entry.rsa_sha256);
            }
            return result;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading manifest.json", e);
        }
    }

    /**
     * Get a file's bytes from the source.
     */
    public byte[] readFileBytes(String relativePath) {
        var filePath = extractedDir.resolve(relativePath);
        if (!Files.isRegularFile(filePath)) { return null; }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading file: " + relativePath, e);
        }
    }

    /**
     * Check if a relative path exists in the source.
     */
    public boolean exists(String relativePath) {
        return Files.exists(extractedDir.resolve(relativePath));
    }

    /**
     * List files within a directory in the source.
     */
    public Stream<Path> listFiles(String relativePath) {
        var dir = extractedDir.resolve(relativePath);
        if (!Files.isDirectory(dir)) { return Stream.empty(); }
        try {
            return Files.walk(dir)
                .filter(Files::isRegularFile)
                .map(p -> extractedDir.relativize(p));
        } catch (IOException e) {
            throw new FcliTechnicalException("Error listing files in: " + relativePath, e);
        }
    }

    /**
     * List immediate subdirectories within a directory.
     */
    public Stream<Path> listDirs(String relativePath) {
        var dir = extractedDir.resolve(relativePath);
        if (!Files.isDirectory(dir)) { return Stream.empty(); }
        try {
            return Files.list(dir).filter(Files::isDirectory);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error listing dirs in: " + relativePath, e);
        }
    }

    @Override
    public void close() {
        if (tempDir) {
            try {
                Files.walk(extractedDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
            } catch (IOException e) {
                LOG.debug("Error cleaning up temp dir: {}", extractedDir, e);
            }
        }
    }

    @com.formkiq.graalvm.annotations.Reflectable
    private static class ManifestEntry {
        public String path;
        public String rsa_sha256;
    }
}
