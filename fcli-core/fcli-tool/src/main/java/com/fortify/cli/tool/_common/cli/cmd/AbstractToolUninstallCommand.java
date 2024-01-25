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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationHelper;
import com.fortify.cli.tool._common.helper.ToolUninstaller;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionRootDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@CommandGroup("uninstall")
public abstract class AbstractToolUninstallCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    private static final ObjectMapper OBJECTMAPPER = JsonHelper.getObjectMapper();
    @Getter @Option(names={"-v", "--versions"}, required = true, split=",", descriptionKey="fcli.tool.uninstall.versions")
    private Set<String> versionsToUninstall;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    
    @Override
    public final JsonNode getJsonNode() {
        try ( var progressWriter = progressWriterFactory.create() ) {
            var runner = new ToolUninstallationRunner(progressWriter, getToolName());
            runner.run();
            return runner.getToolInstallationOutputDescriptors();
        }
    }

    @Override
    public final String getActionCommandResult() {
        return "UNINSTALLED";
    }
    
    @Override
    public final boolean isSingular() {
        return false;
    }
    
    protected abstract String getToolName();
    
    // TODO Remove code duplication between this class and AbstractToolInstallCommand::ToolInstallationPreparer
    private final class ToolUninstallationRunner {
        @Getter private final ArrayNode toolInstallationOutputDescriptors = OBJECTMAPPER.createArrayNode();
        private final IProgressWriterI18n progressWriter;
        private final String toolName;
        private final ToolUninstaller uninstaller;
        private final ToolDefinitionRootDescriptor definitionRootDescriptor;
        
        private ToolUninstallationRunner(IProgressWriterI18n progressWriter, String toolName) {
            this.progressWriter = progressWriter;
            this.toolName = toolName;
            this.uninstaller = new ToolUninstaller(toolName);
            this.definitionRootDescriptor = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName);
        }
        
        @SneakyThrows
        public final void run() {
            Map<String, Runnable> actions = new LinkedHashMap<String, Runnable>();
            addUninstallActions(actions);
            run(actions);
        }

        private final void run(Map<String, Runnable> actions) {
            if ( !actions.isEmpty() ) {
                String msg = String.format("\n  %s", String.join("\n  ", actions.keySet()));
                requireConfirmation.checkConfirmed(msg);
                actions.values().forEach(Runnable::run);
            }
        }

        private final void addUninstallActions(Map<String, Runnable> actions) {
            if ( !versionsToUninstall.isEmpty() ) {
                getVersionsStream()
                    .filter(this::isCandidateForUninstall)
                    .forEach(vd->addUninstallPreparation(vd, actions));
            }
        }

        private final void addUninstallPreparation(ToolDefinitionVersionDescriptor versionDescriptor, Map<String, Runnable> actions) {
            var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
            if ( installationDescriptor!=null ) {
                var msg = String.format("Uninstall %s v%s from %s", toolName, versionDescriptor.getVersion(), installationDescriptor.getInstallDir());
                actions.put(msg, ()->uninstall(versionDescriptor, installationDescriptor));
            }
        }
        
        private final void uninstall(ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor) {
            var toolVersion = versionDescriptor.getVersion();
            var installPath = installationDescriptor.getInstallPath();
            progressWriter.writeProgress("Uninstalling %s %s from %s", toolName, toolVersion, installPath);
            var outputDescriptor = uninstaller.uninstall(versionDescriptor, installationDescriptor);
            toolInstallationOutputDescriptors.add(OBJECTMAPPER.valueToTree(outputDescriptor));
        }
        
        private final Stream<ToolDefinitionVersionDescriptor> getVersionsStream() {
            return definitionRootDescriptor.getVersionsStream();
        }

        private final boolean isCandidateForUninstall(ToolDefinitionVersionDescriptor d) {
            return ToolInstallationHelper.isCandidateForUninstall(toolName, versionsToUninstall, d);
        }
    }
}
