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
package com.fortify.cli.sc_sast.client.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.exception.FcliCommandExecutionException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.util.OutputHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.sc_sast.client.helper.SCSastClientCompatibleVersionHelper;
import com.fortify.cli.sc_sast.sensor_pool.cli.mixin.SCSastSensorPoolResolverMixin;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Builder
record InstallContext(
    UnirestInstance unirest,
    IProgressWriterI18n progressWriter,
    String poolUuid,
    String appVersionId,
    String baseDir,
    String installDirPattern
) {}

@Command(name = OutputHelperMixins.Setup.CMD_NAME)
public class SCSastClientSetupCommand extends AbstractSSCOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin 
    private OutputHelperMixins.Setup outputHelper;
    
    @Mixin
    private ProgressWriterFactoryMixin progressWriterFactory;
    
    @Mixin 
    private SCSastSensorPoolResolverMixin.OptionalOption poolResolver;
    
    @Mixin 
    private SSCAppVersionResolverMixin.OptionalOption appVersionResolver;
    
    @Option(names = {"--base-dir", "-b"}, required = false)
    private String baseDir;
    
    @Option(names = {"--install-dir-pattern"}, required = false)
    private String installDirPattern;
    
    // TODO: Remove hidden=true when support for installing from controller is added
    @Option(names = {"--sources"}, split = ",", defaultValue = "tool-definitions", hidden = true)
    private List<InstallSource> sources;
    
    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return null; // Set by installation source
    }
    
    @Override
    public JsonNode getJsonNode() {
        return install(getUnirestInstance());
    }
    
    private JsonNode install(UnirestInstance unirest) {
        validateMutualExclusivity();
        validateInstallDirOptions();
        
        List<String> errors = new ArrayList<>();
        
        try (var progressWriter = progressWriterFactory.create()) {
            var context = InstallContext.builder()
                    .unirest(unirest)
                    .progressWriter(progressWriter)
                    .poolUuid(poolResolver.getSensorPoolUuid(unirest))
                    .appVersionId(appVersionResolver.getAppVersionId(unirest))
                    .baseDir(baseDir)
                    .installDirPattern(installDirPattern)
                    .build();
            
            for (InstallSource source : sources) {
                try {
                    return source.install(context);
                } catch (Exception e) {
                    errors.add(source.name() + ": " + e.getMessage());
                }
            }
        }
        
        throw new FcliSimpleException(
            "Failed to install ScanCentral Client from all specified sources:\n  " 
            + String.join("\n  ", errors)
        );
    }
    
    private void validateMutualExclusivity() {
        if (poolResolver.hasValue() && appVersionResolver.getAppVersionNameOrId() != null) {
            throw new FcliSimpleException("Cannot specify both --pool and --appversion options");
        }
    }
    
    private void validateInstallDirOptions() {
        if (baseDir != null && installDirPattern != null) {
            throw new FcliSimpleException("--base-dir and --install-dir-pattern are mutually exclusive");
        }
    }
    
    @RequiredArgsConstructor
    public enum InstallSource {
        tool_definitions {
            @Override
            public JsonNode install(InstallContext context) {
                context.progressWriter().writeProgress("Determining compatible ScanCentral Client version...");
                var helper = SCSastClientCompatibleVersionHelper.builder()
                        .unirest(context.unirest())
                        .poolUuid(context.poolUuid())
                        .appVersionId(context.appVersionId())
                        .build();
                String compatibleVersion = helper.getLatestCompatibleVersion();
                context.progressWriter().writeProgress("Registering or installing sc-client " + compatibleVersion + "...");
                return installFromToolDefinitions(compatibleVersion, context);
            }
        };
        
        public abstract JsonNode install(InstallContext context);
        
        private static JsonNode installFromToolDefinitions(String version, InstallContext context) {
            AtomicReference<ObjectNode> resultRecordRef = new AtomicReference<>();
            
            Consumer<ObjectNode> recordConsumer = record -> {
                resultRecordRef.set(record);
            };
            
            Consumer<OutputHelper.Result> onFail = result -> {
                if (result.getErr() != null && !result.getErr().isEmpty()) {
                    System.err.println(result.getErr());
                } else if (result.getOut() != null && !result.getOut().isEmpty()) {
                    System.err.println(result.getOut());
                }
                throw new FcliCommandExecutionException(result);
            };
            
            // Build command with optional directory options
            StringBuilder cmdBuilder = new StringBuilder("tool env init --tools sc-client:").append(version);
            if (context.baseDir() != null) {
                cmdBuilder.append(" --base-dir \"").append(context.baseDir()).append("\"");
            }
            if (context.installDirPattern() != null) {
                cmdBuilder.append(" --install-dir-pattern \"").append(context.installDirPattern()).append("\"");
            }
            
            var result = FcliCommandExecutorFactory.builder()
                    .cmd(cmdBuilder.toString())
                    .stdoutOutputType(OutputType.suppress)
                    .stderrOutputType(OutputType.show)
                    .recordConsumer(recordConsumer)
                    .onFail(onFail)
                    .build()
                    .create()
                    .execute();
            
            if (result.getExitCode() == 0 && resultRecordRef.get() != null) {
                return resultRecordRef.get();
            }
            
            throw new FcliSimpleException("Failed to install ScanCentral Client version " + version);
        }

        @Override
        public String toString() {
            return super.toString().replace('_', '-');
        }
    }
}
