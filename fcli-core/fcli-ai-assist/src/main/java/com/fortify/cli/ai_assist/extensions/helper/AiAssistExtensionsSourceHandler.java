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
package com.fortify.cli.ai_assist.extensions.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.tool.definitions.helper.ToolDefinitionArtifactDescriptor;
import com.fortify.cli.common.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.common.tool.definitions.helper.ToolDefinitionsHelper;

/**
 * Resolves and provides access to extension source contents.
 * Downloads the extensions zip using tool definitions for URL and signature,
 * and reads the distribution descriptor from the tool-definitions zip.
 */
public final class AiAssistExtensionsSourceHandler implements AutoCloseable {
    /** Tool name as registered in tool-definitions */
    static final String TOOL_NAME = "ai-assistant-extensions";
    /** Embedded zip containing the distribution descriptor and its detached signature */
    static final String DISTRIBUTION_ZIP = "ai-assistant-extensions-distribution.zip";
    /** Distribution descriptor file within the embedded zip */
    static final String DISTRIBUTION_FILE = "v1.yaml";
    /** Detached RSA-SHA256 signature for the distribution descriptor */
    static final String DISTRIBUTION_SIG = "v1.yaml.rsa_sha256";

    private static final Logger LOG = LoggerFactory.getLogger(AiAssistExtensionsSourceHandler.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final Path extractedDir;
    private final boolean tempDir;
    private final String version;

    private AiAssistExtensionsSourceHandler(Path extractedDir, boolean tempDir, String version) {
        this.extractedDir = extractedDir;
        this.tempDir = tempDir;
        this.version = version;
    }

    public String getVersion() { return version; }
    public Path getExtractedDir() { return extractedDir; }

    /**
     * Resolve from a local source (directory or zip file), for --source override.
     */
    public static AiAssistExtensionsSourceHandler fromLocalSource(String source) {
        var path = Path.of(source);
        if (Files.isDirectory(path)) {
            return new AiAssistExtensionsSourceHandler(path.toAbsolutePath(), false, "local");
        }
        if (Files.isRegularFile(path)) {
            return fromZipFile(path, "local");
        }
        throw new FcliSimpleException("Source not found: " + source);
    }

    /**
     * Resolve from tool definitions: download the extensions zip for the given
     * version, verify its signature, and extract.
     */
    public static AiAssistExtensionsSourceHandler fromToolDefinitions(
            ToolDefinitionVersionDescriptor versionDesc,
            DigestMismatchAction onDigestMismatch) {
        var artifact = resolveArtifact(versionDesc);
        try {
            var tempZip = Files.createTempFile("fcli-extensions-", ".zip");
            try {
                UnirestHelper.download("ai-assist", artifact.getDownloadUrl(), tempZip.toFile());
                verifyZipSignature(tempZip, artifact, onDigestMismatch);
                return fromZipFile(tempZip, versionDesc.getVersion());
            } finally {
                Files.deleteIfExists(tempZip);
            }
        } catch (IOException e) {
            throw new FcliTechnicalException("Error downloading extensions", e);
        }
    }

    private static ToolDefinitionArtifactDescriptor resolveArtifact(
            ToolDefinitionVersionDescriptor versionDesc) {
        var binaries = versionDesc.getBinaries();
        if (binaries.containsKey("any")) { return binaries.get("any"); }
        if (binaries.size() == 1) { return binaries.values().iterator().next(); }
        throw new FcliSimpleException(
            "Cannot determine artifact for agent-extensions version " + versionDesc.getVersion());
    }

    private static void verifyZipSignature(Path zipPath,
            ToolDefinitionArtifactDescriptor artifact,
            DigestMismatchAction onDigestMismatch) {
        if (artifact.getRsa_sha256() == null) { return; }
        try {
            var fileBytes = Files.readAllBytes(zipPath);
            var status = SignatureHelper.fortifySignatureVerifier()
                .verify(fileBytes, artifact.getRsa_sha256());
            if (status != SignatureStatus.VALID) {
                var msg = "Extensions zip signature verification failed (status: " + status + ")";
                switch (onDigestMismatch) {
                    case fail -> throw new FcliSimpleException(msg);
                    case warn -> LOG.warn("WARNING: {}", msg);
                }
            }
        } catch (IOException e) {
            throw new FcliTechnicalException("Error verifying zip signature", e);
        }
    }

    private static AiAssistExtensionsSourceHandler fromZipFile(Path zipPath, String version) {
        try {
            var tempDir = Files.createTempDirectory("fcli-extensions-");
            try (var zipFile = new ZipFile(zipPath.toFile())) {
                var entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    var entryPath = tempDir.resolve(normalizePath(entry.getName()));
                    if (!entryPath.normalize().startsWith(tempDir)) {
                        throw new FcliSimpleException(
                            "Zip entry contains path traversal: " + entry.getName());
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
            return new AiAssistExtensionsSourceHandler(tempDir, true, version);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error extracting extensions zip: " + zipPath, e);
        }
    }

    private static String normalizePath(String path) {
        if (path.startsWith("./")) { return path.substring(2); }
        return path;
    }

    /**
     * Read and parse the content-manifest.yaml from the extensions zip.
     */
    public AiAssistExtensionsContentManifestDescriptor readContentManifest() {
        var manifestPath = extractedDir.resolve("content-manifest.yaml");
        if (!Files.isRegularFile(manifestPath)) {
            throw new FcliSimpleException("content-manifest.yaml not found in extensions source");
        }
        try {
            return YAML_MAPPER.readValue(manifestPath.toFile(),
                AiAssistExtensionsContentManifestDescriptor.class);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading content-manifest.yaml", e);
        }
    }

    /**
     * Read the distribution descriptor from the nested distribution zip
     * inside tool-definitions.yaml.zip. Verifies the detached RSA-SHA256
     * signature before parsing.
     */
    public static AiAssistExtensionsDistributionDescriptor readDistributionDescriptor(
            boolean verifySignature) {
        try (var zipFs = ToolDefinitionsHelper.openEmbeddedZipFileSystem(DISTRIBUTION_ZIP)) {
            var yamlPath = zipFs.getPath(DISTRIBUTION_FILE);
            var yamlBytes = Files.readAllBytes(yamlPath);
            if (verifySignature) {
                var sigPath = zipFs.getPath(DISTRIBUTION_SIG);
                if (!Files.exists(sigPath)) {
                    throw new FcliSimpleException(
                        "Signature file '" + DISTRIBUTION_SIG + "' not found in distribution zip");
                }
                var signature = Files.readString(sigPath).trim();
                var status = SignatureHelper.fortifySignatureVerifier()
                    .verify(yamlBytes, signature);
                if (status != SignatureStatus.VALID) {
                    LOG.warn("Distribution descriptor signature status: {}", status);
                }
            }
            return YAML_MAPPER.readValue(yamlBytes,
                AiAssistExtensionsDistributionDescriptor.class);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading distribution descriptor", e);
        }
    }

    public byte[] readFileBytes(String relativePath) {
        var filePath = safeResolve(relativePath);
        if (!Files.isRegularFile(filePath)) { return null; }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading file: " + relativePath, e);
        }
    }

    public boolean exists(String relativePath) {
        return Files.exists(safeResolve(relativePath));
    }

    public List<Path> listFiles(String relativePath) {
        var dir = safeResolve(relativePath);
        if (!Files.isDirectory(dir)) { return List.of(); }
        try (var stream = Files.walk(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(p -> extractedDir.relativize(p))
                .toList();
        } catch (IOException e) {
            throw new FcliTechnicalException("Error listing files in: " + relativePath, e);
        }
    }

    public List<Path> listDirs(String relativePath) {
        var dir = safeResolve(relativePath);
        if (!Files.isDirectory(dir)) { return List.of(); }
        try (var stream = Files.list(dir)) {
            return stream.filter(Files::isDirectory).toList();
        } catch (IOException e) {
            throw new FcliTechnicalException("Error listing dirs in: " + relativePath, e);
        }
    }

    private Path safeResolve(String relativePath) {
        var resolved = extractedDir.resolve(relativePath).normalize();
        if (!resolved.startsWith(extractedDir.normalize())) {
            throw new FcliSimpleException(
                "Path traversal detected: " + relativePath);
        }
        return resolved;
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

    /** Digest mismatch handling (mirrors tool module pattern). */
    public enum DigestMismatchAction { warn, fail }
}
