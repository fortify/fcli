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
package com.fortify.cli.tool._common.cli.cmd;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.SemVer;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationHelper;
import com.fortify.cli.tool._common.helper.ToolInstaller;
import com.fortify.cli.tool._common.helper.ToolInstaller.DigestMismatchAction;
import com.fortify.cli.tool._common.helper.ToolInstaller.ToolInstallationResult;
import com.fortify.cli.tool._common.helper.ToolUninstaller;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@CommandGroup("install") @DefaultVariablePropertyName("version")
public abstract class AbstractToolInstallCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    private static final ObjectMapper OBJECTMAPPER = JsonHelper.getObjectMapper();
    @Option(names={"-v", "--version"}, required = false, descriptionKey="fcli.tool.install.version", defaultValue = "latest") 
    private String version;
    @ArgGroup(exclusive = true)
    private InstallOrBaseDirArgGroup installOrBaseDirArgGroup = new InstallOrBaseDirArgGroup();
    @Option(names={"-p", "--platform"}, required = false, descriptionKey="fcli.tool.install.platform") 
    private String platform;
    @Option(names={"--on-digest-mismatch"}, required = false, descriptionKey="fcli.tool.install.on-digest-mismatch", defaultValue = "fail") 
    private DigestMismatchAction onDigestMismatch;
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names={"-u", "--uninstall"}, required = false, split=",",  descriptionKey="fcli.tool.install.uninstall")
    private Set<String> versionsToUninstall = new HashSet<>();
    @Option(names={"--no-global-bin"}, required = false, negatable = true, descriptionKey="fcli.tool.install.global-bin")
    private boolean installGlobalBin = true;
    @Option(names={"--copy-from"}, required = false, descriptionKey="fcli.tool.install.copy-from")
    private File copyFromPath;
    @Option(names={"--on-copy-version-mismatch"}, required = false, descriptionKey="fcli.tool.install.on-copy-version-mismatch", defaultValue = "skip")
    private OnCopyVersionMismatch onCopyVersionMismatch;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    
    public static enum OnCopyVersionMismatch {
        skip, copy, fail
    }
    
    public static class SkipCopyFromException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public SkipCopyFromException() {
            super("Skip copy-from due to version mismatch");
        }
    }
    
    private static final class InstallOrBaseDirArgGroup {
        @Option(names={"-d", "--install-dir"}, required = false, descriptionKey="fcli.tool.install.install-dir") 
        private File installDir;
        @Option(names={"-b", "--base-dir"}, required = false, descriptionKey="fcli.tool.install.base-dir") 
        private File baseDir;
    }
    
    @Override
    public final JsonNode getJsonNode() {
        return install(); 
    }
    
    @Override
    public final String getActionCommandResult() {
        return "INSTALLED";
    }
    
    @Override
    public final boolean isSingular() {
        return true;
    }
    
    protected abstract String getToolName();
    protected abstract void postInstall(ToolInstaller toolInstaller, ToolInstallationResult installationResult);
    protected abstract String getDefaultArtifactType();
    
    /**
     * Get the default binary name for this tool (platform-specific).
     * This is used when --copy-from is specified to locate the binary to copy.
     * Subclasses that support --copy-from must override this method.
     * 
     * @return Binary name (e.g., "fcli", "scancentral", "FodUpload.jar"), or null if copy-from not supported
     */
    protected String getDefaultBinaryName() {
        return null;
    }
    
    /**
     * Check if copy-from mode is enabled
     */
    protected final boolean isCopyFromMode() {
        return copyFromPath != null;
    }
    
    /**
     * Get the copy-from path if specified
     */
    protected final File getCopyFromPath() {
        return copyFromPath;
    }
    
    /**
     * Get the on-copy-version-mismatch setting
     */
    protected final OnCopyVersionMismatch getOnCopyVersionMismatch() {
        return onCopyVersionMismatch;
    }
    
    /**
     * Check if copy-from version matches the requested version pattern.
     * Supports semantic versioning and handles aliases via tool definitions.
     * 
     * @param copyFromVersion The version detected from copy source
     * @param requestedVersion The requested version pattern
     * @param installer The tool installer instance
     * @return true if versions match, false otherwise
     */
    protected final boolean checkCopyFromVersionMatch(String copyFromVersion, String requestedVersion, ToolInstaller installer) {
        // Always match for latest/auto
        if ("latest".equals(requestedVersion) || "auto".equals(requestedVersion)) {
            return true;
        }
        
        // Exact match
        if (copyFromVersion.equals(requestedVersion)) {
            return true;
        }
        
        try {
            var toolDefinition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(getToolName());
            
            // Normalize copy-from version to match tool definition format
            String normalizedCopyFromVersion = toolDefinition.normalizeVersionFormat(copyFromVersion);
            
            // Resolve requested version to actual version descriptor
            var requestedVersionDescriptor = toolDefinition.getVersionOrDefault(requestedVersion);
            String resolvedRequestedVersion = requestedVersionDescriptor.getVersion();
            
            // Check if normalized versions match
            if (normalizedCopyFromVersion.equals(resolvedRequestedVersion)) {
                return true;
            }
            
            // Check if copy-from version matches any alias
            if (requestedVersionDescriptor.getAliases() != null) {
                for (String alias : requestedVersionDescriptor.getAliases()) {
                    if (normalizedCopyFromVersion.equals(alias) || copyFromVersion.equals(alias)) {
                        return true;
                    }
                }
            }
            
            // Fall back to semantic version pattern matching
            return versionMatches(normalizedCopyFromVersion, requestedVersion);
        } catch (Exception e) {
            // If tool definitions unavailable, fall back to semantic version matching
            installer.getProgressWriter().writeWarning(
                "WARN: Unable to verify version compatibility using tool definitions, using pattern matching: " + e.getMessage());
            return versionMatches(copyFromVersion, requestedVersion);
        }
    }
    
    /**
     * Check if a version matches the requested version pattern using semantic versioning.
     * Reuses logic from AbstractToolRegisterCommand.versionMatches.
     * Supports semantic versioning: "2" matches "2.x.y", "2.1" matches "2.1.x", etc.
     * 
     * @param actualVersion The actual detected version
     * @param requestedPattern The requested version pattern
     * @return true if the version matches the pattern
     */
    private boolean versionMatches(String actualVersion, String requestedPattern) {
        // Special handling for "unknown" versions
        if ("unknown".equals(actualVersion)) {
            return "unknown".equals(requestedPattern);
        }
        
        // Normalize by removing 'v' prefix
        String normalizedActual = actualVersion.startsWith("v") ? actualVersion.substring(1) : actualVersion;
        String normalizedRequested = requestedPattern.startsWith("v") ? requestedPattern.substring(1) : requestedPattern;
        
        try {
            SemVer actual = new SemVer(normalizedActual);
            
            // If not a proper semver, fall back to prefix matching
            if (!actual.isProperSemver()) {
                return normalizedActual.startsWith(normalizedRequested);
            }
            
            // Split requested pattern by dots
            String[] requestedParts = normalizedRequested.split("\\.");
            
            // Check major version
            if (requestedParts.length >= 1 && !requestedParts[0].isEmpty()) {
                if (!actual.getMajor().map(m -> m.equals(requestedParts[0])).orElse(false)) {
                    return false;
                }
            }
            
            // Check minor version if specified
            if (requestedParts.length >= 2 && !requestedParts[1].isEmpty()) {
                if (!actual.getMinor().map(m -> m.equals(requestedParts[1])).orElse(false)) {
                    return false;
                }
            }
            
            // Check patch version if specified
            if (requestedParts.length >= 3 && !requestedParts[2].isEmpty()) {
                if (!actual.getPatch().map(p -> p.equals(requestedParts[2])).orElse(false)) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            // If parsing fails, fall back to simple string prefix match
            return normalizedActual.startsWith(normalizedRequested);
        }
    }
    
    /**
     * Subclasses can override this to customize the ToolInstaller builder before installation.
     * This allows registration of hooks for custom version detection or installation logic.
     * 
     * @param builder The ToolInstaller builder to configure
     */
    protected void configureToolInstallerBuilder(ToolInstaller.ToolInstallerBuilder builder) {
        // Set up copy-from logic if enabled
        if (isCopyFromMode()) {
            if (getDefaultBinaryName() == null) {
                throw new FcliSimpleException(
                    "--copy-from is not supported for " + getToolName());
            }
            builder.versionDetector(this::detectVersionFromCopySource);
            builder.installer(this::installFromCopyWithVersionCheck);
        }
    }
    
    /**
     * Install from copy source with version checking.
     * This method checks if the copy-from version matches the requested version
     * and handles version mismatches according to the --on-copy-version-mismatch setting.
     * 
     * If copy-from was skipped (version not in definitions), falls back to download.
     * 
     * @param installer The tool installer
     * @param artifactDescriptor The artifact descriptor (may be null for copy-from mode)
     */
    @SneakyThrows
    private void installFromCopyWithVersionCheck(ToolInstaller installer, Object artifactDescriptor) {
        // Check if copy-from was skipped due to version not in definitions
        if (installer.isSkippedCopyFrom()) {
            // Fall back to default installer (download from tool definitions)
            defaultInstaller(installer, artifactDescriptor);
            return;
        }
        
        String copyFromVersion = installer.getToolVersion();
        String requestedVersion = installer.getRequestedVersion();
        
        boolean versionsMatch = checkCopyFromVersionMatch(copyFromVersion, requestedVersion, installer);
        
        if (!versionsMatch) {
            handleVersionMismatch(copyFromVersion, requestedVersion, installer, artifactDescriptor);
        }
        
        installFromCopy(installer, artifactDescriptor);
    }
    
    /**
     * Handle version mismatch according to the --on-copy-version-mismatch setting.
     * 
     * @param copyFromVersion The version detected from copy source
     * @param requestedVersion The requested version
     * @param installer The tool installer
     * @param artifactDescriptor The artifact descriptor for fallback to download
     */
    private void handleVersionMismatch(String copyFromVersion, String requestedVersion, 
                                      ToolInstaller installer, Object artifactDescriptor) {
        var mismatchAction = getOnCopyVersionMismatch();
        
        switch (mismatchAction) {
            case fail:
                throw new FcliSimpleException(
                    String.format("Version mismatch: --copy-from version is %s but requested version is %s. " +
                        "Use --on-copy-version-mismatch=copy to copy anyway, or --on-copy-version-mismatch=skip to download instead.",
                        copyFromVersion, requestedVersion));
            case skip:
                installer.getProgressWriter().writeWarning(
                    String.format("WARN: Version mismatch: --copy-from version is %s but requested version is %s. " +
                        "Skipping --copy-from and downloading instead (--on-copy-version-mismatch=skip).",
                        copyFromVersion, requestedVersion));
                installer.getProgressWriter().writeProgress(
                    "Downloading %s %s", installer.getToolName(), installer.getToolVersion());
                defaultInstaller(installer, artifactDescriptor);
                throw new SkipCopyFromException(); // Prevent installFromCopy from executing
            case copy:
                installer.getProgressWriter().writeWarning(
                    String.format("WARN: Version mismatch: --copy-from version is %s but requested version is %s. " +
                        "Proceeding with copy anyway (--on-copy-version-mismatch=copy).",
                        copyFromVersion, requestedVersion));
                break;
        }
    }
    
    /**
     * Fall back to default installer (download and extract).
     * This is called when copy-from is skipped due to version mismatch.
     */
    @SneakyThrows
    private void defaultInstaller(ToolInstaller installer, Object artifactDescriptor) {
        if (artifactDescriptor instanceof com.fortify.cli.tool.definitions.helper.ToolDefinitionArtifactDescriptor) {
            // Use reflection to access private downloadAndExtract method
            var method = ToolInstaller.class.getDeclaredMethod(
                "downloadAndExtract", com.fortify.cli.tool.definitions.helper.ToolDefinitionArtifactDescriptor.class);
            method.setAccessible(true);
            method.invoke(installer, artifactDescriptor);
        } else {
            throw new FcliSimpleException("Cannot fall back to download: artifact descriptor is not a ToolDefinitionArtifactDescriptor");
        }
    }
    
    /**
     * Detect version from copy source.
     * Default implementation looks for fcli install descriptor in the source directory.
     * Subclasses can override for more advanced version detection (e.g., executing --version).
     * 
     * @param installer The tool installer
     * @return Detected version string
     * @throws FcliSimpleException if version cannot be detected
     */
    protected String detectVersionFromCopySource(ToolInstaller installer) {
        File sourceDir = resolveCopySourceDirectory();
        String detectedVersion = detectVersionFromInstallDescriptor(sourceDir);
        if (detectedVersion == null) {
            throw new FcliSimpleException(
                "Cannot detect version from --copy-from path. Only tools previously installed through fcli are supported " +
                "for --copy-from unless the tool command provides a custom implementation.");
        }
        return detectedVersion;
    }
    
    /**
     * Install from copy source.
     * Default implementation copies the entire installation directory tree from an fcli-installed tool.
     * Subclasses can override for more advanced copy logic (e.g., selective file copying).
     * 
     * @param installer The tool installer
     * @param artifactDescriptor The artifact descriptor (may be null for copy-from mode)
     */
    @SneakyThrows
    protected void installFromCopy(ToolInstaller installer, Object artifactDescriptor) {
        File sourceDir = resolveCopySourceDirectory();
        Path sourcePath = sourceDir.toPath();
        Path targetPath = installer.getTargetPath();
        
        installer.getProgressWriter().writeProgress(
            "Copying %s from %s to %s", getToolName(), sourcePath, targetPath);
        
        Files.createDirectories(targetPath);
        copyDirectoryContents(sourcePath, targetPath);
    }
    
    /**
     * Resolve the copy source directory.
     * Default implementation resolves the install directory from --copy-from path by looking for
     * the directory containing an 'install-descriptor' subdirectory.
     * Subclasses can override for custom resolution logic.
     * 
     * @return The resolved source directory
     * @throws FcliSimpleException if directory cannot be resolved
     */
    protected File resolveCopySourceDirectory() {
        File copyFromFile = getCopyFromPath();
        
        // If it's a file (e.g., binary), get parent directories until we find install-descriptor
        File searchDir = copyFromFile.isFile() ? copyFromFile.getParentFile() : copyFromFile;
        
        // Search upward for install-descriptor directory
        File current = searchDir;
        while (current != null) {
            File installDescriptorDir = new File(current, "install-descriptor");
            if (installDescriptorDir.exists() && installDescriptorDir.isDirectory()) {
                return current;
            }
            current = current.getParentFile();
        }
        
        throw new FcliSimpleException(
            "Cannot find fcli install descriptor in --copy-from path or its parent directories. " +
            "Only tools previously installed through fcli are supported for --copy-from unless " +
            "the tool command provides a custom implementation.");
    }
    
    /**
     * Detect version from install descriptor in source directory.
     * 
     * @param sourceDir Source installation directory
     * @return Version string from descriptor, or null if not found
     */
    private String detectVersionFromInstallDescriptor(File sourceDir) {
        File installDescriptorDir = new File(sourceDir, "install-descriptor");
        if (!installDescriptorDir.exists() || !installDescriptorDir.isDirectory()) {
            return null;
        }
        
        // Look for {tool-name}/{version} structure
        File toolDir = new File(installDescriptorDir, getToolName());
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
     * Copy directory contents recursively.
     * 
     * @param source Source directory
     * @param target Target directory
     */
    @SneakyThrows
    private void copyDirectoryContents(Path source, Path target) {
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        // Preserve executable permissions
                        if (Files.isExecutable(sourcePath)) {
                            targetPath.toFile().setExecutable(true);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to copy " + sourcePath + " to " + target, e);
                }
            });
    }
    
    private final ArrayNode install() {
        try ( var progressWriter = progressWriterFactory.create() ) {
            var preparer = new ToolInstallationPreparer();
            var builder = ToolInstaller.builder()
                    .defaultPlatform(getDefaultArtifactType())
                    .onDigestMismatch(onDigestMismatch)
                    .preInstallAction(preparer)
                    .postInstallAction(this::postInstall)
                    .progressWriter(progressWriter)
                    .targetPathProvider(this::getTargetPath)
                    .globalBinPathProvider(this::getGlobalBinPath)
                    .toolName(getToolName())
                    .requestedVersion(version);
            configureToolInstallerBuilder(builder);
            var installer = builder.build();
            var installResult = StringUtils.isBlank(platform) ? installer.install() : installer.install(platform);
            var result = OBJECTMAPPER.createArrayNode();
            result.add(OBJECTMAPPER.valueToTree(installResult.asOutputDescriptor()));
            result.addAll(preparer.getToolInstallationOutputDescriptors());
            return result;
        }
    }
    
    private final Path getInstallPath() {
        return installOrBaseDirArgGroup.installDir==null
                ? null 
                : installOrBaseDirArgGroup.installDir.toPath();
    }
    
    private final Path getBasePath() {
        var basePath = installOrBaseDirArgGroup.baseDir==null
                ? null 
                : installOrBaseDirArgGroup.baseDir.toPath();
        if ( getInstallPath()==null && basePath==null ) {
            basePath = Path.of(EnvHelper.getUserHome(), "fortify", "tools"); 
        }
        return basePath; 
    }

    private final Path getTargetPath(ToolInstaller toolInstaller) {
        var installPath = getInstallPath();
        Path result = null;
        if ( installPath!=null ) {
            toolInstaller.getProgressWriter().writeWarning("WARN: --install-dir option is deprecated");
            result = installPath;
        } else {
            var basePath = getBasePath();
            result = basePath.resolve(String.format("%s/%s", getToolName(), toolInstaller.getToolVersion()));
        }
        return result.normalize().toAbsolutePath();
    }

    private final Path getGlobalBinPath(ToolInstaller toolInstaller) {
        var basePath = getBasePath(); 
        return basePath==null || !installGlobalBin ? null : basePath.resolve("bin");
    }
    
    private final class ToolInstallationPreparer implements Consumer<ToolInstaller> {
        @Getter private final ArrayNode toolInstallationOutputDescriptors = OBJECTMAPPER.createArrayNode();
        private ToolInstaller installer;
        private ToolUninstaller uninstaller;
        
        @Override
        public void accept(ToolInstaller installer) {
            this.installer = installer;
            this.uninstaller = new ToolUninstaller(installer.getToolName());
            prepare();
        }
        
        @SneakyThrows
        private final void prepare() {
            Map<String, Runnable> requiredPreparations = new LinkedHashMap<String, Runnable>();
            addTargetDirPreparation(requiredPreparations);
            addUninstallPreparations(requiredPreparations);
            prepare(requiredPreparations);
        }

        private final void prepare(Map<String, Runnable> requiredPreparations) {
            if ( !requiredPreparations.isEmpty() ) {
                // Generate message for prompt. This includes the required preparation actions
                // from requiredPreparations, and for clarity, also the installation action.
                String msg = String.format("\n  %s\n  Install %s %s to %s", 
                        String.join("\n  ", requiredPreparations.keySet()),
                        installer.getToolName(), installer.getToolVersion(), installer.getTargetPath());
                requireConfirmation.checkConfirmed(msg);
                requiredPreparations.values().forEach(Runnable::run);
            }
        }
        
        @SneakyThrows
        private final void addTargetDirPreparation(Map<String, Runnable> requiredPreparations) {
            var targetPath = installer.getTargetPath();
            if ( Files.exists(targetPath) ) {
                if ( ToolInstallationDescriptor.optionalCopyFromToolInstallPath(targetPath, installer.getToolName(), installer.getVersionDescriptor())==null ) {
                    var existingVersionsWithSameTargetPath = getVersionsStream()
                                .filter(d->!isCandidateForUninstall(d))
                                .filter(d->installer.hasMatchingTargetPath(d))
                                .map(ToolDefinitionVersionDescriptor::getVersion)
                                .collect(Collectors.toList());
                    var otherVersionsWithSameTargetPath = existingVersionsWithSameTargetPath.stream()
                            .filter(v->!v.equals(installer.getToolVersion()))
                            .collect(Collectors.toList());
                    if ( !otherVersionsWithSameTargetPath.isEmpty() ) {
                        throw new FcliSimpleException(String.format("Target path %s already in use for versions: %s\nUse --replace option to explicitly uninstall the existing versions", targetPath, String.join(", ", otherVersionsWithSameTargetPath)));
                    } else if ( existingVersionsWithSameTargetPath.isEmpty() ) {
                        // Basically we're moving the tool installation to a different directory
                        requiredPreparations.put("Clean target directory "+targetPath, ()->deleteRecursive(targetPath));
                    }
                }
            }
        }

        private final void addUninstallPreparations(Map<String, Runnable> requiredPreparations) {
            if ( !versionsToUninstall.isEmpty() ) {
                getVersionsStream()
                    .filter(this::isCandidateForUninstall)
                    .forEach(vd->addUninstallPreparation(vd, requiredPreparations));
            }
        }

        private final void addUninstallPreparation(ToolDefinitionVersionDescriptor versionDescriptor, Map<String, Runnable> requiredPreparations) {
            var toolName = installer.getToolName();
            var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
            if ( installationDescriptor!=null ) {
                var msg = String.format("Uninstall %s %s from %s", toolName, versionDescriptor.getVersion(), installationDescriptor.getInstallDir());
                requiredPreparations.put(msg, ()->uninstall(versionDescriptor, installationDescriptor));
            }
        }
        
        private final void deleteRecursive(Path targetPath) {
            installer.getProgressWriter().writeProgress("Cleaning target directory %s", targetPath);
            FileUtils.deleteRecursive(targetPath);
        }
        
        private final void uninstall(ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor) {
            var toolName = installer.getToolName();
            var toolVersion = versionDescriptor.getVersion();
            var installPath = installationDescriptor.getInstallPath();
            installer.getProgressWriter().writeProgress("Uninstalling %s %s from %s", toolName, toolVersion, installPath);
            var outputDescriptor = uninstaller.uninstall(versionDescriptor, installationDescriptor, installer.getVersionDescriptor());
            toolInstallationOutputDescriptors.add(OBJECTMAPPER.valueToTree(outputDescriptor));
        }
        
        private final Stream<ToolDefinitionVersionDescriptor> getVersionsStream() {
            return installer.getDefinitionRootDescriptor().getVersionsStream();
        }

        /**
         * The given version descriptor is considered a candidate for uninstall
         * if all of the following conditions are met:
         * - {@link ToolInstallationHelper#isCandidateForUninstall(String, Set, ToolDefinitionVersionDescriptor)}
         *   returns true
         * - The version doesn't match the target version to be installed, 
         *   or target path is different from existing installation
         */
        private final boolean isCandidateForUninstall(ToolDefinitionVersionDescriptor d) {
            var toolName = installer.getToolName();
            return ToolInstallationHelper.isCandidateForUninstall(toolName, versionsToUninstall, d)
                    && !(d.getVersion().equals(installer.getToolVersion()) && installer.hasMatchingTargetPath(d));
        }
    }
}
