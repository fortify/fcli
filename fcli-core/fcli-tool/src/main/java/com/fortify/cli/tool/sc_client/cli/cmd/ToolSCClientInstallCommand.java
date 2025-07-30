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
package com.fortify.cli.tool.sc_client.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool._common.helper.ToolInstaller;
import com.fortify.cli.tool._common.helper.ToolInstaller.BinScriptType;
import com.fortify.cli.tool._common.helper.ToolInstaller.DigestMismatchAction;
import com.fortify.cli.tool._common.helper.ToolInstaller.ToolInstallationResult;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionArtifactDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class ToolSCClientInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolSCClientCommands.TOOL_NAME;
    @Option(names= {"-t", "--client-auth-token"}) private String clientAuthToken; 
    @Option(names= {"--with-jre"}) private boolean withJre;
    @Option(names= {"--jre-platform"}) private String jrePlatform;
    
    @Override
    protected String getDefaultArtifactType() {
        return "java";
    }
    
    @Override @SneakyThrows
    protected void postInstall(ToolInstaller installer, ToolInstallationResult installationResult) {
        updateClientAuthToken(installer.getTargetPath());
        if ( withJre || StringUtils.isNotBlank(jrePlatform) ) { installJre(installer); }
        installer.installGlobalBinScript(BinScriptType.bash, "scancentral", "bin/scancentral");
        installer.installGlobalBinScript(BinScriptType.bat, "scancentral.bat", "bin/scancentral.bat");
        installer.installGlobalBinScript(BinScriptType.bash, "pwtool", "bin/pwtool");
        installer.installGlobalBinScript(BinScriptType.bat, "pwtool.bat", "bin/pwtool.bat");
    }
    
    private void updateClientAuthToken(Path installPath) throws IOException {
        if ( StringUtils.isNotBlank(clientAuthToken) ) {
            Path clientPropertiesPath = installPath.resolve("Core/config/client.properties");
            Files.writeString(clientPropertiesPath, 
                    String.format("client_auth_token=%s", clientAuthToken), 
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
    
    private void installJre(ToolInstaller scClientInstaller) throws IOException {
        var platform = StringUtils.isNotBlank(jrePlatform) ? jrePlatform : ToolPlatformHelper.getPlatform();
        new SCClientJREInstaller(scClientInstaller).installJre(platform);
    }
    
    @RequiredArgsConstructor // TODO Remove code duplication between this class and ToolInstaller
    private static final class SCClientJREInstaller {
        private final ToolInstaller scClientInstaller;
        
        public void installJre(String platform) throws IOException {
            var jreTargetPath = getJreTargetPath();
            if ( !Files.exists(jreTargetPath) || !Files.list(jreTargetPath).findFirst().isPresent() ) {
                var jreVersion = getJreVersion();
                var jreBinaryDescriptor = getJreArtifactDescriptor(jreVersion, platform);
                downloadAndExtractJre(jreBinaryDescriptor);
                updateExecPermissions(jreTargetPath);
            }
        }

        private void updateExecPermissions(Path jreTargetPath) {
            FileUtils.setAllFilePermissions(jreTargetPath.resolve("bin"), FileUtils.execPermissions, false);
            var jspawnhelper = jreTargetPath.resolve("lib/jspawnhelper");
            if ( Files.exists(jspawnhelper) ) {
                FileUtils.setSinglePathPermissions(jspawnhelper, FileUtils.execPermissions);
            }
        }
        
        private void downloadAndExtractJre(ToolDefinitionArtifactDescriptor jreArtifactDescriptor) throws IOException {
            scClientInstaller.getProgressWriter().writeProgress("Downloading ScanCentral Client JRE");
            File downloadedFile = downloadJre(jreArtifactDescriptor);
            scClientInstaller.getProgressWriter().writeProgress("Verifying JRE signature");
            SignatureHelper.fortifySignatureVerifier()
                .verify(downloadedFile, jreArtifactDescriptor.getRsa_sha256())
                .throwIfNotValid(scClientInstaller.getOnDigestMismatch() == DigestMismatchAction.fail);
            scClientInstaller.getProgressWriter().writeProgress("Installing JRE binaries");
            extractJre(jreArtifactDescriptor, downloadedFile);
        }
        
        private static final File downloadJre(ToolDefinitionArtifactDescriptor jreArtifactDescriptor) throws IOException {
            File tempDownloadFile = File.createTempFile("fcli-tool-download", null);
            tempDownloadFile.deleteOnExit();
            UnirestHelper.download("tool", jreArtifactDescriptor.getDownloadUrl(), tempDownloadFile);
            return tempDownloadFile;
        }
        
        private final void extractJre(ToolDefinitionArtifactDescriptor jreArtifactDescriptor, File downloadedFile) throws IOException {
            Path targetPath = getJreTargetPath();
            Files.createDirectories(targetPath);
            var artifactName = jreArtifactDescriptor.getName();
            if (artifactName.endsWith("gz") || artifactName.endsWith(".tar.gz")) {
                FileUtils.extractTarGZ(downloadedFile, FileUtils.defaultExtractPathResolver(targetPath, this::rewriteExtractSourcePath));
            } else if (artifactName.endsWith("zip")) {
                FileUtils.extractZip(downloadedFile, FileUtils.defaultExtractPathResolver(targetPath, this::rewriteExtractSourcePath)); 
            }
            downloadedFile.delete();
        }
        
        private final Path rewriteExtractSourcePath(Path p) {
            return Path.of(p.toString().replaceAll("^jdk-.*-jre[/\\\\]", ""));
        }

        private ToolDefinitionArtifactDescriptor getJreArtifactDescriptor(String jreVersion, String platform) {
            var toolDefinitions = ToolDefinitionsHelper.getToolDefinitionRootDescriptor("jre");
            var jreVersionDescriptor = toolDefinitions.getVersion(jreVersion);
            var jreBinaryDescriptor = jreVersionDescriptor.getBinaries().get(platform);
            if ( jreBinaryDescriptor==null ) { throw new FcliSimpleException("No JRE found for platform "+platform); }
            return jreBinaryDescriptor;
        }

        private String getJreVersion() {
            var versionDescriptor = scClientInstaller.getVersionDescriptor();
            var extraProperties = versionDescriptor.getExtraProperties(); 
            var jreVersion = extraProperties==null ? null : extraProperties.get("jre");
            if ( StringUtils.isBlank(jreVersion) ) {
                throw new FcliSimpleException("Tool definitions don't list JRE version for this ScanCentral Client version; cannot install JRE as requested");
            }
            return jreVersion;
        }
        
        private Path getJreTargetPath() {
            return scClientInstaller.getTargetPath().resolve("jre");
        }
    }
}
