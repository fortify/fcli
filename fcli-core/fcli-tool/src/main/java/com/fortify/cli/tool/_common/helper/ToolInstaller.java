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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
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
    @Getter private final Function<ToolInstaller,Path> globalBinPathProvider;
    @Getter private final DigestMismatchAction onDigestMismatch;
    @Getter private final Consumer<ToolInstaller> preInstallAction;
    @Getter private final BiConsumer<ToolInstaller, ToolInstallationResult> postInstallAction;
    @Getter private final IProgressWriterI18n progressWriter;
    private final LazyObject<ToolDefinitionRootDescriptor> _definitionRootDescriptor = new LazyObject<>();
    private final LazyObject<ToolDefinitionVersionDescriptor> _versionDescriptor = new LazyObject<>();
    private final LazyObject<ToolInstallationDescriptor> _previousInstallationDescriptor = new LazyObject<>();
    private final LazyObject<Path> _targetPath = new LazyObject<>();
    private final LazyObject<Path> _globalBinPath = new LazyObject<>();
    
    @Data
    public static final class ToolInstallationResult {
        private final String toolName;
        private final ToolDefinitionVersionDescriptor versionDescriptor;
        private final ToolDefinitionArtifactDescriptor artifactDescriptor;
        private final ToolInstallationDescriptor installationDescriptor;
        
        public final ToolInstallationOutputDescriptor asOutputDescriptor() {
            return new ToolInstallationOutputDescriptor(toolName, versionDescriptor, installationDescriptor, "INSTALLED");
        }
    }
    
    public static enum DigestMismatchAction {
        fail, warn
    }
    
    public static enum GlobalBinScriptType {
        bash, bat
    }
    
    public final ToolDefinitionRootDescriptor getDefinitionRootDescriptor() {
        return _definitionRootDescriptor.get(()->ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName));
    }
    
    public final ToolDefinitionVersionDescriptor getVersionDescriptor() {
        return _versionDescriptor.get(()->getDefinitionRootDescriptor().getVersionOrDefault(requestedVersion));
    }
    
    public final ToolInstallationDescriptor getPreviousInstallationDescriptor() {
        return _previousInstallationDescriptor.get(()->ToolInstallationDescriptor.load(toolName, getVersionDescriptor()));
    }
    
    public final Path getTargetPath() {
        return _targetPath.get(()->targetPathProvider.apply(this));
    }
    
    public final Path getBinPath() {
        return getTargetPath().resolve("bin");
    }
    
    public final Path getGlobalBinPath() {
        return _globalBinPath.get(()->globalBinPathProvider.apply(this));
    }
    
    public final String getToolVersion() {
        return getVersionDescriptor().getVersion();
    }
    
    public final boolean hasMatchingTargetPath(ToolDefinitionVersionDescriptor versionDescriptor) {
        var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
        var currentToolInstallPath = installationDescriptor==null ? null: installationDescriptor.getInstallPath().normalize();
        var targetToolInstallPath = getTargetPath().normalize();
        return targetToolInstallPath.equals(currentToolInstallPath) && Files.exists(targetToolInstallPath);
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
    
    public final void copyBinResource(String resourceFile) {
        var fileName = Paths.get(resourceFile).getFileName().toString();
        Path targetFilePath = getBinPath().resolve(fileName);
        if (!Files.exists(targetFilePath)) {
            // We don't reinstall bin resources if they already exist for two reasons:
            // - If the script already exists, it means that we're doing an update instead
            //   of full install, so we want to make sure that the scripts match the current
            //   install. For example, suppose fcli was first installed using --platform linux/x64
            //   and later 're-installed' with --platform java, we'd be installing scripts for
            //   Java even though we didn't actually install the jar-file.
            // - On Windows, updating existing batch files while running can cause strange behavior.
            //   For example, suppose fcli 3.0.0 was installed from fcli 2.2.0, and a 're-install' 
            //   for fcli 3.0.0 is done using fcli 3.0.0. If fcli 3.0.0 would overwrite the existing
            //   batch files with different contents, this could cause incorrect behavior and likely
            //   error messages once Windows resumes batch file execution once fcli has finished.
            var fullResourceFile = getFullResourceFile(resourceFile);
            FileUtils.copyResource(fullResourceFile, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    @SneakyThrows
    public final void installGlobalBinScript(GlobalBinScriptType type, String globalBinScriptName, String target) {
        var globalBinPath = getGlobalBinPath();
        if ( globalBinPath!=null ) {
            var resourceFile = ToolInstallationHelper.getToolResourceLocation("extra-files/global-bin/"+type.name());
            var globalBinFilePath = globalBinPath.resolve(globalBinScriptName);
            var globalBinTargetFilePath = getTargetPath().resolve(target);
            if ( Files.exists(globalBinTargetFilePath) ) {
                FileUtils.copyResource(resourceFile, globalBinFilePath, StandardCopyOption.REPLACE_EXISTING);
                String content = new String(Files.readAllBytes(globalBinFilePath), "ASCII");
                content = content.replace("{{target}}", globalBinTargetFilePath.toString());
                Files.write(globalBinFilePath, content.getBytes("ASCII"));
                ToolInstaller.updateFilePermissions(globalBinFilePath);
            }
        }
    }
    
    private String getFullResourceFile(String resourceFile) {
        return ToolInstallationHelper.getToolResourceLocation(String.format("%s/%s", getToolName().replace("-", "_"), resourceFile));
    }
    
    private final ToolInstallationResult install(ToolDefinitionArtifactDescriptor artifactDescriptor) {
        try {
            preInstallAction.accept(this);
            var versionDescriptor = getVersionDescriptor();
            warnIfDifferentTargetPath();
            if ( !hasMatchingTargetPath(getVersionDescriptor()) ) {
                checkEmptyTargetPath();
                downloadAndExtract(artifactDescriptor);
            }
            var result = new ToolInstallationResult(toolName, versionDescriptor, artifactDescriptor, createAndSaveInstallationDescriptor());
            progressWriter.writeProgress("Running post-install actions");
            postInstallAction.accept(this, result);
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
    
    private final ToolInstallationDescriptor createAndSaveInstallationDescriptor() {
        var installPath = getTargetPath();
        var binPath = getBinPath();
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
    private final void warnIfDifferentTargetPath() {
        var oldDescriptor = getPreviousInstallationDescriptor();
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
        
    // TODO Move this method to FileUtils or similar, as it's also used by AbstractToolInstallCommand
    @SneakyThrows
    public static final void updateFilePermissions(Path p) {
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
