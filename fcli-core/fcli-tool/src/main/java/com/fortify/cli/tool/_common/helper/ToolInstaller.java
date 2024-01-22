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
package com.fortify.cli.tool._common.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionArtifactDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionRootDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;

@Builder
public final class ToolInstaller {
    private static final Set<PosixFilePermission> binPermissions = PosixFilePermissions.fromString("rwxr-xr-x");
    @Getter private final String toolName;
    @Getter private final String requestedVersion;
    @Getter private final String defaultPlatform;
    @Getter private final Function<ToolInstaller,Path> targetPathProvider;
    @Getter private final DigestMismatchAction onDigestMismatch;
    @Getter private final Consumer<ToolInstaller> preInstallAction;
    @Getter private final Consumer<ToolInstallationResult> postInstallAction;
    @Getter private final IProgressWriterI18n progressWriter;
    private final LazyObject<ToolDefinitionRootDescriptor> _definitionRootDescriptor = new LazyObject<>();
    private final LazyObject<ToolDefinitionVersionDescriptor> _versionDescriptor = new LazyObject<>();
    private final LazyObject<Path> _targetPath = new LazyObject<>();
    
    @Data
    public static final class ToolInstallationResult {
        private final String toolName;
        private final ToolDefinitionVersionDescriptor versionDescriptor;
        private final ToolDefinitionArtifactDescriptor artifactDescriptor;
        private final ToolInstallationDescriptor installationDescriptor;
        
        public final ToolInstallationOutputDescriptor asOutputDescriptor() {
            return new ToolInstallationOutputDescriptor(toolName, versionDescriptor, installationDescriptor);
        }
    }
    
    public static enum DigestMismatchAction {
        fail, warn
    }
    
    public final ToolDefinitionRootDescriptor getDefinitionRootDescriptor() {
        return _definitionRootDescriptor.get(()->ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName));
    }
    
    public final ToolDefinitionVersionDescriptor getVersionDescriptor() {
        return _versionDescriptor.get(()->getDefinitionRootDescriptor().getVersionOrDefault(requestedVersion));
    }
    
    public final Path getTargetPath() {
        return _targetPath.get(()->targetPathProvider.apply(this));
    }
    
    public final String getToolVersion() {
        return getVersionDescriptor().getVersion();
    }
    
    public final ToolInstallationResult install() {
        var artifactDescriptor = getArtifactDescriptor(ToolPlatformHelper.getPlatform())
                .orElseGet(()->getArtifactDescriptor(defaultPlatform)
                        .orElseThrow(()->new IllegalStateException("Appropriate artifact for system platform cannot be determined automatically, please specify platform explicitly")));
        return install(artifactDescriptor);
    }
    
    public final ToolInstallationResult install(String platform) {
        var artifactDescriptor = getArtifactDescriptor(platform)
                .orElseThrow(()->new IllegalStateException(String.format("No matching artifact found for platform %s", platform)));
        return install(artifactDescriptor);
    }
    
    private final ToolInstallationResult install(ToolDefinitionArtifactDescriptor artifactDescriptor) {
        try {
            preInstallAction.accept(this);
            var versionDescriptor = getVersionDescriptor();
            var previousInstallationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
            warnIfDifferentTargetPath(previousInstallationDescriptor);
            checkEmptyTargetPath();
            downloadAndExtract(artifactDescriptor);
            var result = new ToolInstallationResult(toolName, versionDescriptor, artifactDescriptor, createAndSaveInstallationDescriptor());
            progressWriter.writeProgress("Running post-install actions");
            postInstallAction.accept(result);
            updateBinPermissions(result.getInstallationDescriptor().getBinPath());
            return result;
        } catch ( IOException e ) {
            throw new RuntimeException("Error installing "+toolName, e);
        }
    }

    private void downloadAndExtract(ToolDefinitionArtifactDescriptor artifactDescriptor) throws IOException {
        progressWriter.writeProgress("Downloading tool binaries");
        File downloadedFile = download(artifactDescriptor);
        progressWriter.writeProgress("Verifying signature");
        SignatureHelper.verifyFileSignature(downloadedFile, artifactDescriptor.getRsa_sha256(), onDigestMismatch == DigestMismatchAction.fail);
        progressWriter.writeProgress("Installing tool binaries");
        copyOrExtract(artifactDescriptor, downloadedFile);
    }
    
    private static final File download(ToolDefinitionArtifactDescriptor artifactDescriptor) throws IOException {
        File tempDownloadFile = File.createTempFile("fcli-tool-download", null);
        tempDownloadFile.deleteOnExit();
        UnirestHelper.download("tool", artifactDescriptor.getDownloadUrl(), tempDownloadFile);
        return tempDownloadFile;
    }
    
    private final void copyOrExtract(ToolDefinitionArtifactDescriptor artifactDescriptor, File downloadedFile) throws IOException {
        Path targetPath = getTargetPath();
        Files.createDirectories(targetPath);
        var artifactName = artifactDescriptor.getName();
        if (artifactName.endsWith("gz") || artifactName.endsWith(".tar.gz")) {
            FileUtils.extractTarGZ(downloadedFile, targetPath);
        } else if (artifactDescriptor.getName().endsWith("zip")) {
            FileUtils.extractZip(downloadedFile, targetPath); 
        } else {
            Files.copy(downloadedFile.toPath(), targetPath.resolve(artifactDescriptor.getName()), StandardCopyOption.REPLACE_EXISTING);
        }
        downloadedFile.delete();
    }
    
    private ToolInstallationDescriptor createAndSaveInstallationDescriptor() {
        var installPath = getTargetPath();
        var binPath = installPath.resolve("bin");
        var installationDescriptor = new ToolInstallationDescriptor(installPath, binPath);
        installationDescriptor.save(toolName, getVersionDescriptor());
        return installationDescriptor;
    }
    
    private final Optional<ToolDefinitionArtifactDescriptor> getArtifactDescriptor(String platform) {
        return StringUtils.isBlank(platform) 
                ? Optional.empty() 
                : Optional.ofNullable(getVersionDescriptor().getBinaries().get(platform));   
    }
    
    @SneakyThrows
    private final void warnIfDifferentTargetPath(ToolInstallationDescriptor oldDescriptor) {
        var targetPath = getTargetPath();
        if ( oldDescriptor!=null && !oldDescriptor.getInstallPath().toAbsolutePath().equals(targetPath.toAbsolutePath()) ) {
            String msg = "WARN: This tool version was previously installed in another directory." +
                       "\n      Fcli will only track the latest installation directory; you may" +
                       "\n      want to manually remove the old installation directory." +
                       "\n        Old: "+oldDescriptor.getInstallDir() +
                       "\n        New: "+targetPath;
            progressWriter.writeWarning(msg);
        }
    }
    
    private final void checkEmptyTargetPath() throws IOException {
        var targetPath = getTargetPath();
        if ( Files.exists(targetPath) && Files.list(targetPath).findFirst().isPresent() ) {
            throw new IllegalStateException("Non-empty target path "+targetPath+" already exists");
        }
    }
    
    private static final void updateBinPermissions(Path binPath) throws IOException {
        try (Stream<Path> walk = Files.walk(binPath)) {
            walk.forEach(ToolInstaller::updateFilePermissions);
        }
    }
        
    @SneakyThrows
    private static final void updateFilePermissions(Path p) {
        try {
            Files.setPosixFilePermissions(p, binPermissions);
        } catch ( UnsupportedOperationException e ) {
            // Log warning?
        }
    }
    
    // TODO Is there a standard Java class for this?
    private static final class LazyObject<T> {
        private T value = null;
        public T get(Supplier<T> supplier) {
            if ( value==null ) {
                value = supplier.get();
            }
            return value;
        }
    }
}
