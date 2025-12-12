/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.tool._common.helper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.PlatformHelper;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionArtifactDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionRootDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public final class ToolInstaller {
    @Getter private final String toolName;
    @Getter private final String requestedVersion;
    @Getter private final String fallbackPlatform;
    @Getter private final Function<ToolInstaller,Path> targetPathProvider;
    @Getter private final Function<ToolInstaller,Path> globalBinPathProvider;
    @Getter private final DigestMismatchAction onDigestMismatch;
    @Getter private final Consumer<ToolInstaller> preInstallAction;
    @Getter private final BiConsumer<ToolInstaller, ToolInstallationResult> postInstallAction;
    @Getter private final IProgressWriterI18n progressWriter;
    
    // Copy-if-matching configuration
    @Getter private final File copyIfMatchingPath;
    @Getter private final BiFunction<ToolInstaller, File, String> toolVersionDetectorCallback;
    @Getter private final Function<File, File> installDirResolver;
    
    @Getter @Builder.Default private final Function<ToolInstaller,String> versionDetector = ToolInstaller::defaultVersionDetector;
    @Getter @Builder.Default private final BiConsumer<ToolInstaller,ToolDefinitionArtifactDescriptor> installer = ToolInstaller::defaultInstaller;
    private final LazyObject<ToolDefinitionRootDescriptor> _definitionRootDescriptor = new LazyObject<>();
    private final LazyObject<ToolDefinitionVersionDescriptor> _versionDescriptor = new LazyObject<>();
    private final LazyObject<ToolInstallationDescriptor> _previousInstallationDescriptor = new LazyObject<>();
    private final LazyObject<Path> _targetPath = new LazyObject<>();
    private final LazyObject<Path> _globalBinPath = new LazyObject<>();
    private boolean skippedCopyIfMatching;
    
    public static enum ToolInstallationAction {
        INSTALLED, SKIPPED_EXISTING, COPIED
    }
    
    @Data @Builder
    public static final class ToolInstallationResult {
        private final String toolName;
        private final ToolDefinitionVersionDescriptor versionDescriptor;
        private final ToolDefinitionArtifactDescriptor artifactDescriptor;
        private final ToolInstallationDescriptor installationDescriptor;
        @JsonProperty(IActionCommandResultSupplier.actionFieldName) private final ToolInstallationAction action;
        
        public final ToolInstallationOutputDescriptor asOutputDescriptor() {
            return new ToolInstallationOutputDescriptor(toolName, versionDescriptor, installationDescriptor, action.name(), true);
        }
    }
    
    public static enum DigestMismatchAction {
        fail, warn
    }
    
    public static enum BinScriptType {
        bash, bat
    }
    
    public final ToolDefinitionRootDescriptor getDefinitionRootDescriptor() {
        return _definitionRootDescriptor.get(()->ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName));
    }
    
    public final ToolDefinitionVersionDescriptor getVersionDescriptor() {
        return _versionDescriptor.get(()->{
            String detectedVersion = versionDetector.apply(this);
            var rootDescriptor = getDefinitionRootDescriptor();
            
            // Try to find version in definitions
            try {
                return rootDescriptor.getVersionOrDefault(detectedVersion);
            } catch (Exception e) {
                // Version not found in definitions - treat as non-matching and use requested version
                log.info("Detected version {} from --copy-if-matching not found in tool definitions. " +
                    "Downloading requested version {} instead.",
                    detectedVersion, requestedVersion);
                skippedCopyIfMatching = true;
                return rootDescriptor.getVersionOrDefault(requestedVersion);
            }
        });
    }
    
    public final boolean isSkippedCopyIfMatching() {
        return skippedCopyIfMatching;
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
        return _globalBinPath.get(()->globalBinPathProvider==null?null:globalBinPathProvider.apply(this));
    }
    
    public final String getToolVersion() {
        return getVersionDescriptor().getVersion();
    }
    
    public final boolean hasMatchingTargetPath(ToolDefinitionVersionDescriptor versionDescriptor) {
        var installationDescriptor = ToolInstallationDescriptor.optionalCopyFromToolInstallPath(getTargetPath(), toolName, versionDescriptor);
        var currentToolInstallPath = installationDescriptor==null ? null: installationDescriptor.getInstallPath().normalize();
        var targetToolInstallPath = getTargetPath().normalize();
        return targetToolInstallPath.equals(currentToolInstallPath) && Files.exists(targetToolInstallPath);
    }
    
    public final ToolInstallationResult install() {
        var artifactDescriptor = getArtifactDescriptor(PlatformHelper.getPlatform())
                .orElseGet(()->getArtifactDescriptor(fallbackPlatform)
                        .orElseThrow(()->new IllegalStateException("Appropriate artifact for system platform cannot be determined automatically, please specify platform explicitly")));
        return install(artifactDescriptor);
    }
    
    public final ToolInstallationResult install(String platform) {
        var artifactDescriptor = getArtifactDescriptor(platform)
                .orElseThrow(()->new IllegalStateException(String.format("No matching artifact found for platform %s", platform)));
        return install(artifactDescriptor);
    }
    
    private static String defaultVersionDetector(ToolInstaller installer) {
        return installer.requestedVersion;
    }
    
    @SneakyThrows
    private static void defaultInstaller(ToolInstaller installer, ToolDefinitionArtifactDescriptor artifactDescriptor) {
        installer.downloadAndExtract(artifactDescriptor);
    }
    
    /**
     * This method can be called by Tool*InstallCommands to install bin-scripts for 
     * Java-based tools. It will install both the tool version specific bin-scripts 
     * and global bin-scripts (if applicable). If global bin scripts already exist, 
     * they will be replaced. If tool version specific bin-scripts already exist, 
     * they will not be replaced, for the following reasons:
     * <ul>
     *   <li>If the script already exists, it means that we're doing an update instead
     *       of full install, so we want to make sure that the scripts match the current
     *       install. For example, suppose fcli was first installed using --platform linux/x64
     *       and later 're-installed' with --platform java, we'd be installing scripts for
     *       Java even though we didn't actually install the jar-file.</li>
     *   <li>On Windows, updating existing batch files while running can cause strange behavior.
     *       For example, suppose fcli 3.0.0 was installed from fcli 2.2.0, and a 're-install' 
     *       for fcli 3.0.0 is done using fcli 3.0.0. If fcli 3.0.0 would overwrite the existing
     *       batch files with different contents, this could cause incorrect behavior and likely
     *       error messages once Windows resumes batch file execution once fcli has finished.</li>
     * </ul>
     * @param scriptBaseName Base name (without extension) for the scripts to be installed
     * @param binScriptTargetJar Path to the jar-file, relative to the tool installation directory
     */
    public final void installJavaBinScripts(String scriptBaseName, String binScriptTargetJar) {
        var scriptTargetFilePath = getTargetPath().resolve(binScriptTargetJar);
        if ( !Files.exists(scriptTargetFilePath) ) {
            throw new FcliSimpleException("Cannot install bin scripts; target jar doesn't exist: "+scriptTargetFilePath);
        }
        for ( var type : BinScriptType.values() ) {
            var resourceFile = ToolInstallationHelper.getResourcePath("extra-files/java-bin/"+type.name());
            var replacements = getResourceReplacementsMap(getTargetPath(), scriptTargetFilePath);
            var scriptName = type==BinScriptType.bash ? scriptBaseName : scriptBaseName+".bat";
            installResource(resourceFile, getBinPath().resolve(scriptName), false, replacements);
            installGlobalBinScript(type, scriptName, "bin/"+scriptName);
        }
    }
    
    /**
     * Install a global bin-script of the given {@link BinScriptType} with the given globalBinScriptName.
     * The installed script will invoke the given globalBinScriptTarget. The script will only be installed
     * if a global bin path has been configured. Note that for Java-based tools, the 
     * {@link #installJavaBinScripts(String, String)} method should be used instead, which installs
     * both tool version specific bin-scripts and corresponding global bin-scripts.
     * @param type Type of the bin script; bash or bat
     * @param globalBinScriptName Name of the script to be installed to the global bin directory
     * @param globalBinScriptTarget Target script/executable to be invoked by the global bin-script,
     *        relative to the tool installation directory.
     */
    @SneakyThrows
    public final void installGlobalBinScript(BinScriptType type, String globalBinScriptName, String globalBinScriptTarget) {
        var globalBinPath = getGlobalBinPath();
        if ( globalBinPath!=null ) {
            var resourceFile = ToolInstallationHelper.getResourcePath("extra-files/global-bin/"+type.name());
            var globalBinScriptPath = globalBinPath.resolve(globalBinScriptName);
            var scriptTargetFilePath = getTargetPath().resolve(globalBinScriptTarget);
            if ( Files.exists(scriptTargetFilePath) ) {
                var replacements = getResourceReplacementsMap(globalBinPath.getParent(), scriptTargetFilePath);
                installResource(resourceFile, globalBinScriptPath, true, replacements);
                FileUtils.setSinglePathPermissions(globalBinScriptPath, FileUtils.execPermissions);
            }
        }
    }
    
    
    private final ToolInstallationResult install(ToolDefinitionArtifactDescriptor artifactDescriptor) {
        try {
            if ( preInstallAction!=null ) { preInstallAction.accept(this); }
            var versionDescriptor = getVersionDescriptor();
            warnIfDifferentTargetPath();
            ToolInstallationAction action;
            if ( !hasMatchingTargetPath(getVersionDescriptor()) ) {
                checkEmptyTargetPath();
                installer.accept(this, artifactDescriptor);
                action = determineInstallationAction();
            } else {
                action = ToolInstallationAction.SKIPPED_EXISTING;
            }
            // Always save descriptor (even when installation was skipped) to update timestamp,
            // making this the default version for 'tool run' commands
            var result = ToolInstallationResult.builder()
                .toolName(toolName)
                .versionDescriptor(versionDescriptor)
                .artifactDescriptor(artifactDescriptor)
                .installationDescriptor(createAndSaveInstallationDescriptor())
                .action(action)
                .build();
            if ( postInstallAction!=null ) {
                progressWriter.writeProgress("Running post-install actions");
                postInstallAction.accept(this, result);
            }
            FileUtils.setAllFilePermissions(result.getInstallationDescriptor().getBinPath(), FileUtils.execPermissions, false);
            return result;
        } catch ( IOException e ) {
            throw new FcliSimpleException("Error installing "+toolName, e);
        }
    }
    
    private ToolInstallationAction determineInstallationAction() {
        if (copyIfMatchingPath != null && !skippedCopyIfMatching) {
            return ToolInstallationAction.COPIED;
        }
        return ToolInstallationAction.INSTALLED;
    }

    private void downloadAndExtract(ToolDefinitionArtifactDescriptor artifactDescriptor) throws IOException {
        progressWriter.writeProgress("Downloading tool binaries");
        File downloadedFile = download(artifactDescriptor);
        progressWriter.writeProgress("Verifying signature");
        SignatureHelper.fortifySignatureVerifier()
            .verify(downloadedFile, artifactDescriptor.getRsa_sha256())
            .throwIfNotValid(onDigestMismatch == DigestMismatchAction.fail);
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
        var globalBinPath = getGlobalBinPath();
        var installationDescriptor = new ToolInstallationDescriptor(installPath, binPath, globalBinPath);
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
            throw new FcliSimpleException("Non-empty target path "+targetPath+" already exists");
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
    
    @SneakyThrows
    private final void installResource(String resourceFile, Path targetPath, boolean replaceExisting, Map<String, String> replacements) {
        if ( replaceExisting || !Files.exists(targetPath) ) {
            String contents = FileUtils.readResourceAsString(resourceFile, StandardCharsets.US_ASCII);
            if ( replacements!=null ) {
                contents = replacements.entrySet().stream()
                    .map(entry -> (Function<String, String>) data -> data.replace(entry.getKey(), entry.getValue()))
                    .reduce(Function.identity(), Function::andThen)
                    .apply(contents);
            }
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, contents.getBytes("ASCII"));
        }
    }
    
    private final Map<String, String> getResourceReplacementsMap(Path basePath, Path targetPath) {
        Map<String, String> result = new HashMap<>();
        var relativePath = basePath.toAbsolutePath().normalize().relativize(targetPath.toAbsolutePath().normalize());
        var relativePathString = relativePath.toString();
        result.put("{{relativeBashTargetPath}}", relativePathString);
        result.put("{{relativeBatTargetPath}}", relativePathString.replace('/', '\\'));
        return result;
    }
    
    // ===== Copy-if-matching logic methods =====
    
    /**
     * Configures the ToolInstaller builder with copy-if-matching functionality.
     * Copy will only occur if the detected version matches the requested version.
     */
    public static ToolInstallerBuilder configureCopyIfMatching(
            ToolInstallerBuilder builder, 
            File copyIfMatchingPath,
            Function<File, File> installDirResolver,
            BiFunction<ToolInstaller, File, String> toolVersionDetectorCallback) {
        return builder
            .copyIfMatchingPath(copyIfMatchingPath)
            .toolVersionDetectorCallback(toolVersionDetectorCallback)
            .installDirResolver(installDirResolver)
            .versionDetector(ToolInstaller::copyIfMatchingVersionDetector)
            .installer(ToolInstaller::copyIfMatchingInstaller);
    }
    
    /**
     * Resolves the install directory from the copy-if-matching path.
     * Uses the installDirResolver callback if provided, otherwise uses default logic.
     */
    private File resolveInstallDirectory(File copyIfMatchingPath) {
        if (copyIfMatchingPath == null) {
            return null;
        }
        
        if (installDirResolver != null) {
            return installDirResolver.apply(copyIfMatchingPath);
        }
        
        // Default: use ToolRegistrationHelper logic
        return ToolRegistrationHelper.resolveInstallDir(copyIfMatchingPath);
    }
    
    /**
     * Detects version from install descriptor subdirectory.
     */
    private String detectVersionFromInstallDescriptor(File installDir) {
        File installDescriptorDir = new File(installDir, "install-descriptor");
        if (!installDescriptorDir.exists() || !installDescriptorDir.isDirectory()) {
            return null;
        }
        
        // Look for {tool-name}/{version} structure
        File toolDir = new File(installDescriptorDir, toolName);
        if (!toolDir.exists() || !toolDir.isDirectory()) {
            return null;
        }
        
        File[] versionFiles = toolDir.listFiles(File::isFile);
        if (versionFiles != null && versionFiles.length > 0) {
            // Return the first version file name (there should only be one)
            return versionFiles[0].getName();
        }
        
        return null;
    }
    
    /**
     * Detects version from copy source using descriptor first, then callback.
     * Returns null on any errors to allow graceful fallback to download.
     */
    private String detectVersionFromCopySource(File installDir) {
        try {
            String detectedVersion = detectVersionFromInstallDescriptor(installDir);
            if (detectedVersion == null && toolVersionDetectorCallback != null) {
                detectedVersion = toolVersionDetectorCallback.apply(this, installDir);
            }
            return detectedVersion;
        } catch (Exception e) {
            log.debug("Error detecting version from copy source: " + installDir, e);
            return null;
        }
    }
    
    /**
     * Detects version from copy source.
     * Falls back to requested version if detection fails, skipping the copy.
     */
    private String copyIfMatchingVersionDetector() {
        try {
            var resolvedCopyPath = resolveInstallDirectory(copyIfMatchingPath);
            if (resolvedCopyPath == null) {
                log.debug("Could not resolve install directory from: {}", copyIfMatchingPath);
                skippedCopyIfMatching = true;
                return requestedVersion;
            }
            
            String detectedVersion = detectVersionFromCopySource(resolvedCopyPath);
            
            if (detectedVersion == null) {
                log.debug("Unable to detect version from copy source: {}", copyIfMatchingPath);
                skippedCopyIfMatching = true;
                return requestedVersion;
            }
            
            return detectedVersion;
        } catch (Exception e) {
            log.debug("Error in copyIfMatchingVersionDetector", e);
            skippedCopyIfMatching = true;
            return requestedVersion;
        }
    }
    
    /**
     * Checks if copy source version matches requested version via tool definitions.
     */
    private boolean checkCopyIfMatchingVersionMatch(String requestedVersion, String detectedVersion) {
        // Exact match
        if (requestedVersion.equals(detectedVersion)) {
            return true;
        }
        
        try {
            // Check if requestedVersion is an alias that resolves to detectedVersion
            var versionDescriptor = getDefinitionRootDescriptor()
                .getVersionOrDefault(requestedVersion);
            
            if (versionDescriptor != null && detectedVersion.equals(versionDescriptor.getVersion())) {
                return true;
            }
        } catch (Exception e) {
            // Version lookup failed (e.g., version not in definitions)
            // Treat as non-matching to allow graceful fallback
            log.debug("Failed to resolve requested version in tool definitions: {}", requestedVersion, e);
        }
        
        return false;
    }
    
    /**
     * Copies directory contents recursively.
     */
    @SneakyThrows
    private void copyDirectoryContents(Path source, Path target) {
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    var targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new FcliTechnicalException("Error copying " + sourcePath + " to " + target, e);
                }
            });
        }
    }
    
    /**
     * Installer implementation for copy-if-matching functionality.
     * Falls back to regular download if copy is not possible or version doesn't match.
     */
    @SneakyThrows
    private void copyIfMatchingInstaller(ToolDefinitionArtifactDescriptor artifactDescriptor) {
        // Check if copy was already skipped during version detection
        if (skippedCopyIfMatching) {
            progressWriter.writeProgress("Skipping copy, downloading instead");
            downloadAndExtract(artifactDescriptor);
            return;
        }
        
        var resolvedCopyPath = resolveInstallDirectory(copyIfMatchingPath);
        if (resolvedCopyPath == null) {
            progressWriter.writeProgress("Copy source directory not found, downloading instead");
            downloadAndExtract(artifactDescriptor);
            return;
        }
        
        String detectedVersion = detectVersionFromCopySource(resolvedCopyPath);
        String requestedVersion = getRequestedVersion();
        
        if (detectedVersion != null && !checkCopyIfMatchingVersionMatch(requestedVersion, detectedVersion)) {
            progressWriter.writeProgress(
                "Version mismatch (requested: " + requestedVersion + ", detected: " + detectedVersion + "). Skipping copy, will download instead.");
            downloadAndExtract(artifactDescriptor);
            return;
        }
        
        progressWriter.writeProgress("Copying from: " + resolvedCopyPath);
        copyDirectoryContents(resolvedCopyPath.toPath(), getTargetPath());
        progressWriter.writeProgress("Copy complete");
    }
    
}
