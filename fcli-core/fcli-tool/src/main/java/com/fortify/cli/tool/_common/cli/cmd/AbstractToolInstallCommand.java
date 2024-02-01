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
package com.fortify.cli.tool._common.cli.cmd;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationHelper;
import com.fortify.cli.tool._common.helper.ToolInstaller;
import com.fortify.cli.tool._common.helper.ToolInstaller.DigestMismatchAction;
import com.fortify.cli.tool._common.helper.ToolInstaller.ToolInstallationResult;
import com.fortify.cli.tool._common.helper.ToolUninstaller;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@CommandGroup("install")
public abstract class AbstractToolInstallCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    private static final ObjectMapper OBJECTMAPPER = JsonHelper.getObjectMapper();
    @Option(names={"-v", "--version"}, required = true, descriptionKey="fcli.tool.install.version", defaultValue = "latest") 
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
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    
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
        return false;
    }
    
    protected abstract String getToolName();
    protected abstract void postInstall(ToolInstaller toolInstaller, ToolInstallationResult installationResult);
    protected abstract String getDefaultArtifactType();
    
    private final ArrayNode install() {
        try ( var progressWriter = progressWriterFactory.create() ) {
            var preparer = new ToolInstallationPreparer();
            var installer = ToolInstaller.builder()
                    .defaultPlatform(getDefaultArtifactType())
                    .onDigestMismatch(onDigestMismatch)
                    .preInstallAction(preparer)
                    .postInstallAction(this::postInstall)
                    .progressWriter(progressWriter)
                    .targetPathProvider(this::getTargetPath)
                    .globalBinPathProvider(this::getGlobalBinPath)
                    .toolName(getToolName())
                    .requestedVersion(version)
                    .build();
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
            basePath = Path.of(System.getProperty("user.home"),"fortify", "tools"); 
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
                var existingVersionsWithSameTargetPath = getVersionsStream()
                            .filter(d->!isCandidateForUninstall(d))
                            .filter(d->installer.hasMatchingTargetPath(d))
                            .map(ToolDefinitionVersionDescriptor::getVersion)
                            .collect(Collectors.toList());
                var otherVersionsWithSameTargetPath = existingVersionsWithSameTargetPath.stream()
                        .filter(v->!v.equals(installer.getToolVersion()))
                        .collect(Collectors.toList());
                if ( !otherVersionsWithSameTargetPath.isEmpty() ) {
                    throw new IllegalStateException(String.format("Target path %s already in use for versions: %s\nUse --replace option to explicitly uninstall the existing versions", targetPath, String.join(", ", otherVersionsWithSameTargetPath)));
                } else if ( existingVersionsWithSameTargetPath.isEmpty() ) {
                    // Basically we're moving the tool installation to a different directory
                    requiredPreparations.put("Clean target directory "+targetPath, ()->deleteRecursive(targetPath));
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
