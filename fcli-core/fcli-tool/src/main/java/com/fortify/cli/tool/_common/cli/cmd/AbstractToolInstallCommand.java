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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool._common.helper.OsAndArchHelper;
import com.fortify.cli.tool._common.helper.SignatureHelper;
import com.fortify.cli.tool._common.helper.ToolHelper;
import com.fortify.cli.tool._common.helper.ToolDefinitionArtifactDescriptor;
import com.fortify.cli.tool._common.helper.ToolOutputDescriptor;
import com.fortify.cli.tool._common.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public abstract class AbstractToolInstallCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractToolInstallCommand.class);
    private static final Set<PosixFilePermission> binPermissions = PosixFilePermissions.fromString("rwxr-xr-x");
    @Getter @Option(names={"-v", "--version"}, required = true, descriptionKey="fcli.tool.install.version", defaultValue = "latest") 
    private String version;
    @Getter @Option(names={"-d", "--install-dir"}, required = false, descriptionKey="fcli.tool.install.install-dir") 
    private File installDir;
    @Getter @Option(names={"--type"}, required = false, descriptionKey="fcli.tool.install.type") 
    private String type;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    @Getter @Option(names={"--on-digest-mismatch"}, required = false, descriptionKey="fcli.tool.install.on-digest-mismatch", defaultValue = "fail") 
    private DigestMismatchAction onDigestMismatch;
    
    private static enum DigestMismatchAction {
        fail, warn
    }
    
    @Override
    public final JsonNode getJsonNode() {
        return new ObjectMapper().<ObjectNode>valueToTree(downloadAndInstall()); 
    }
    
    @Override
    public String getActionCommandResult() {
        return "INSTALLED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private final ToolOutputDescriptor downloadAndInstall() {
        try {
            String toolName = getToolName();
            ToolDefinitionVersionDescriptor versionDescriptor = ToolHelper.getToolDefinitionRootDescriptor(toolName).getVersionOrDefault(version);
            var installationDescriptor = createInstallationDescriptor(toolName, getVersion());
            warnIfDifferentInstallPath(installationDescriptor, ToolHelper.loadToolInstallationDescriptor(toolName, versionDescriptor));
            emptyExistingInstallPath(installationDescriptor.getInstallPath());
            ToolDefinitionArtifactDescriptor artifactDescriptor = getArtifactDescriptor(versionDescriptor, this.type);
            File downloadedFile = download(artifactDescriptor);
            SignatureHelper.verifyFileSignature(downloadedFile, artifactDescriptor.getRsa_sha256(), onDigestMismatch == DigestMismatchAction.fail);
            install(versionDescriptor, artifactDescriptor, installationDescriptor, downloadedFile);
            ToolHelper.saveToolInstallationDescriptor(toolName, versionDescriptor, installationDescriptor);
            return new ToolOutputDescriptor(toolName, version, versionDescriptor, installationDescriptor);            
        } catch ( IOException e ) {
            throw new RuntimeException("Error installing "+getToolName(), e);
        }
    }
    
    private final void install(ToolDefinitionVersionDescriptor versionDescriptor, ToolDefinitionArtifactDescriptor artifactDescriptor, ToolInstallationDescriptor installationDescriptor, File downloadedFile) throws IOException {
        Path installPath = installationDescriptor.getInstallPath();
        Files.createDirectories(installPath);
        InstallType installType = getInstallType(versionDescriptor, artifactDescriptor);
        switch (installType) {
        // TODO Clean this up
        case COPY: Files.copy(downloadedFile.toPath(), installPath.resolve(artifactDescriptor.getName()), StandardCopyOption.REPLACE_EXISTING); 
            break;
        case EXTRACT_ZIP: FileUtils.extractZip(downloadedFile, installPath); 
            break;
        case EXTRACT_TGZ: FileUtils.extractTarGZ(downloadedFile, installPath); 
            break;
        default: throw new RuntimeException("Unknown install type: "+installType.name());
        }
        downloadedFile.delete();
        postInstall(versionDescriptor, artifactDescriptor, installationDescriptor);
        updateBinPermissions(installationDescriptor.getBinPath());
    }
    
    private ToolInstallationDescriptor createInstallationDescriptor(String toolName, String version) {
        var installPath = getInstallPath(toolName, version);
        var binPath = getBinPath(toolName, version);
        var installationDescriptor = new ToolInstallationDescriptor(installPath, binPath);
        return installationDescriptor;
    }

    @SneakyThrows
    private final Path getInstallPath(String toolName, String version) {
        var result = this.installDir;
        if ( result == null ) {
            result = FcliDataHelper.getFortifyHomePath().resolve(String.format("tools/%s/%s", toolName, version)).toFile();
        }
        return result.getCanonicalFile().toPath();
    }
    
    private final Path getBinPath(String toolName, String version) {
        return getInstallPath(toolName, version).resolve("bin");
    }
    
    protected abstract String getToolName();
    
    protected InstallType getInstallType(ToolDefinitionVersionDescriptor descriptor, ToolDefinitionArtifactDescriptor artifactDescriptor) {
        String artifactName = artifactDescriptor.getName();
        if(artifactName.endsWith("gz") || artifactName.endsWith(".tar.gz")) {
            return InstallType.EXTRACT_TGZ;
        } else if(artifactDescriptor.getName().endsWith("zip")) {
            return InstallType.EXTRACT_ZIP;
        } else {
            return InstallType.COPY;
        }
    };
    protected abstract void postInstall(ToolDefinitionVersionDescriptor versionDescriptor, ToolDefinitionArtifactDescriptor artifactDescriptor, ToolInstallationDescriptor installDescriptor) throws IOException;
    
    private final void emptyExistingInstallPath(Path path) throws IOException {
        if ( Files.exists(path) && Files.list(path).findFirst().isPresent() ) {
            requireConfirmation.checkConfirmed();
            FileUtils.deleteRecursive(path);
        }
    }
    
    @SneakyThrows
    private static final void warnIfDifferentInstallPath(ToolInstallationDescriptor newDescriptor, ToolInstallationDescriptor oldDescriptor) {
        if ( oldDescriptor!=null && !oldDescriptor.getInstallPath().toAbsolutePath().equals(newDescriptor.getInstallPath().toAbsolutePath()) ) {
            String msg = "WARN: This tool version was previously installed in another directory." +
                       "\n      Fcli will only track the latest installation directory; you may" +
                       "\n      want to manually remove the old installation directory." +
                       "\n        Old: "+oldDescriptor.getInstallDir() +
                       "\n        New: "+newDescriptor.getInstallDir();
            LOG.warn(msg);
        }
    }
    
    private static final File download(ToolDefinitionArtifactDescriptor artifactDescriptor) throws IOException {
        File tempDownloadFile = File.createTempFile("fcli-tool-download", null);
        tempDownloadFile.deleteOnExit();
        download(artifactDescriptor.getDownloadUrl(), tempDownloadFile);
        return tempDownloadFile;
    }
    
    private static final void download(String downloadUrl, File destFile) {
        UnirestInstance unirest = GenericUnirestFactory.getUnirestInstance("tool",
                u->ProxyHelper.configureProxy(u, "tool", downloadUrl));
        unirest.get(downloadUrl).asFile(destFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING).getBody();
    }
    
    private static final ToolDefinitionArtifactDescriptor getArtifactDescriptor(ToolDefinitionVersionDescriptor downloadDescriptor, String type) {
        if(StringUtils.isBlank(type)) {
            String OSString = OsAndArchHelper.getOSString();
            String archString = OsAndArchHelper.getArchString();
            type = OSString + "/" + archString;
        }
        var artifacts = downloadDescriptor.getArtifacts();
        var result = artifacts.computeIfAbsent(type, t->artifacts.get("default"));
        if ( result==null ) {
            throw new RuntimeException("No default or matching artifact found for type " + type);
        }
        return result;
    }
    
    private static final void updateBinPermissions(Path binPath) throws IOException {
        try (Stream<Path> walk = Files.walk(binPath)) {
            walk.forEach(AbstractToolInstallCommand::updateFilePermissions);
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
    
    protected static enum InstallType {
        EXTRACT_ZIP, EXTRACT_TGZ, COPY
    }
}
