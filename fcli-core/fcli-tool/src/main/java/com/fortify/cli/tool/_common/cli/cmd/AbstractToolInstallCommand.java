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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationHelper;
import com.fortify.cli.tool._common.helper.ToolInstallationOutputDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstaller;
import com.fortify.cli.tool._common.helper.ToolInstaller.DigestMismatchAction;
import com.fortify.cli.tool._common.helper.ToolInstaller.ToolInstallationResult;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@CommandGroup("install")
public abstract class AbstractToolInstallCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    private static final ObjectMapper OBJECTMAPPER = JsonHelper.getObjectMapper();
    @Option(names={"-v", "--version"}, required = true, descriptionKey="fcli.tool.install.version", defaultValue = "latest") 
    private String version;
    @Option(names={"-d", "--install-dir"}, required = false, descriptionKey="fcli.tool.install.install-dir") 
    private File installDir;
    @Option(names={"-b", "--base-dir"}, required = false, descriptionKey="fcli.tool.install.base-dir") 
    private File baseDir;
    @Option(names={"-p", "--platform"}, required = false, descriptionKey="fcli.tool.install.platform") 
    private String platform;
    @Option(names={"--on-digest-mismatch"}, required = false, descriptionKey="fcli.tool.install.on-digest-mismatch", defaultValue = "fail") 
    private DigestMismatchAction onDigestMismatch;
    @Option(names={"-r", "--replace"}, required = false, negatable = true, descriptionKey="fcli.tool.install.replace")
    private boolean replace = false;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    
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
    protected abstract void postInstall(ToolInstallationResult installationResult);
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

    private final Path getTargetPath(ToolInstaller toolInstaller) {
        Path result = null;
        if ( this.installDir!=null ) {
            toolInstaller.getProgressWriter().writeWarning("WARN: --install-dir option is deprecated");
            result = this.installDir.toPath();
        } else {
            var basePath = this.baseDir!=null
                    ? this.baseDir.toPath()
                    : Path.of(System.getProperty("user.home"),"fortify", "tools");
            result = basePath.resolve(String.format("%s/%s", getToolName(), toolInstaller.getToolVersion()));
        }
        return result.normalize().toAbsolutePath();
    }
    
    private final class ToolInstallationPreparer implements Consumer<ToolInstaller> {
        @Getter private final ArrayNode toolInstallationOutputDescriptors = OBJECTMAPPER.createArrayNode();
        private ToolInstaller installer;
        
        @Override
        public void accept(ToolInstaller installer) {
            this.installer = installer;
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
            if ( Files.exists(targetPath) && Files.list(targetPath).findFirst().isPresent() ) {
                requiredPreparations.put("Clean target directory "+targetPath, ()->deleteRecursive(targetPath));
            }
        }

        private final void addUninstallPreparations(Map<String, Runnable> requiredPreparations) {
            if ( replace ) {
                installer.getDefinitionRootDescriptor().getVersionsStream()
                    .filter(v->!v.getVersion().equals(installer.getVersionDescriptor().getVersion()))
                    .forEach(versionDescriptor->addUninstallPreparation(versionDescriptor, requiredPreparations));
            }
        }

        private final void addUninstallPreparation(ToolDefinitionVersionDescriptor versionDescriptor, Map<String, Runnable> requiredPreparations) {
            var toolName = installer.getToolName();
            var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
            if ( installationDescriptor!=null ) {
                var msg = String.format("Uninstall %s v%s from %s", toolName, versionDescriptor.getVersion(), installationDescriptor.getInstallDir());
                requiredPreparations.put(msg, ()->uninstall(versionDescriptor, installationDescriptor));
            }
        }
        
        private void deleteRecursive(Path targetPath) {
            installer.getProgressWriter().writeProgress("Cleaning target directory %s", targetPath);
            FileUtils.deleteRecursive(targetPath);
        }
        
        private void uninstall(ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor) {
            var toolName = installer.getToolName();
            var toolVersion = versionDescriptor.getVersion();
            var installDir = installationDescriptor.getInstallDir();
            installer.getProgressWriter().writeProgress("Uninstalling %s %s from %s", toolName, toolVersion, installDir);
            ToolInstallationHelper.uninstall(installer.getToolName(), versionDescriptor, installationDescriptor);
            ObjectNode output = OBJECTMAPPER.valueToTree(new ToolInstallationOutputDescriptor(toolName, versionDescriptor, installationDescriptor));
            output.put("__action__", "UNINSTALLED");
            toolInstallationOutputDescriptors.add(output);
        }
    }
}
