package com.fortify.cli.tool.common.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.runner.GenericUnirestRunner;
import com.fortify.cli.common.util.FcliHomeHelper;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool.common.helper.ToolHelper;
import com.fortify.cli.tool.common.helper.ToolVersionCombinedDescriptor;
import com.fortify.cli.tool.common.helper.ToolVersionDownloadDescriptor;
import com.fortify.cli.tool.common.helper.ToolVersionInstallDescriptor;
import com.fortify.cli.tool.common.util.FileUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ReflectiveAccess @FixInjection
public abstract class AbstractToolInstallCommand extends AbstractBasicOutputCommand implements IActionCommandResultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractToolInstallCommand.class);
    private static final Set<PosixFilePermission> binPermissions = PosixFilePermissions.fromString("rwxr-xr-x");
    @Getter @Parameters(index="0", arity="0..1", descriptionKey="fcli.tool.install.version", defaultValue = "default") 
    private String version;
    @Getter @Option(names={"-d", "--install-dir"}, required = false, descriptionKey="fcli.tool.install.install-dir") 
    private String installDir;
    @Getter @Option(names={"-y", "--replace-existing"}, required = false, descriptionKey="fcli.tool.install.replace") 
    private boolean replaceExisting;
    @Getter @Option(names={"--on-digest-mismatch"}, required = false, descriptionKey="fcli.tool.install.on-digest-mismatch", defaultValue = "fail") 
    private DigestMismatchAction onDigestMismatch;
    @Inject private GenericUnirestRunner unirestRunner; 
    
    private static enum DigestMismatchAction {
        fail, warn
    }
    
    @Override
    protected final JsonNode getJsonNode() {
        String toolName = getToolName();
        ToolVersionDownloadDescriptor descriptor = ToolHelper.getToolDownloadDescriptor(toolName).getVersionOrDefault(version);
        return downloadAndInstall(toolName, descriptor);
    }
    
    @Override
    public String getActionCommandResult() {
        return "INSTALLED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private final JsonNode downloadAndInstall(String toolName, ToolVersionDownloadDescriptor downloadDescriptor) {
        try {
            String installDir = getInstallDirOrDefault(downloadDescriptor);
            String binDir = getBinDir(downloadDescriptor);
            ToolVersionInstallDescriptor installDescriptor = new ToolVersionInstallDescriptor(downloadDescriptor, installDir, binDir);
            emptyExistingInstallPath(installDescriptor.getInstallPath());
            File downloadedFile = download(downloadDescriptor);
            checkDigest(downloadDescriptor, downloadedFile);
            install(installDescriptor, downloadedFile);
            ToolVersionCombinedDescriptor combinedDescriptor = ToolHelper.saveToolVersionInstallDescriptor(toolName, installDescriptor);
            return new ObjectMapper().<ObjectNode>valueToTree(combinedDescriptor);            
        } catch ( IOException e ) {
            throw new RuntimeException("Error installing "+getToolName(), e);
        }
    }

    private final File download(ToolVersionDownloadDescriptor descriptor) throws IOException {
        File tempDownloadFile = File.createTempFile("fcli-tool-download", null);
        tempDownloadFile.deleteOnExit();
        unirestRunner.run(u->download(u, descriptor.getDownloadUrl(), tempDownloadFile));
        return tempDownloadFile;
    }
    
    private final Void download(UnirestInstance unirest, String downloadUrl, File destFile) {
        ProxyHelper.configureProxy(unirest, "tool", downloadUrl);
        unirest.get(downloadUrl).asFile(destFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING).getBody();
        return null;
    }
    
    protected void install(ToolVersionInstallDescriptor descriptor, File downloadedFile) throws IOException {
        Path installPath = Paths.get(descriptor.getInstallDir());
        Files.createDirectories(installPath);
        InstallType installType = getInstallType();
        switch (installType) {
        // TODO Clean this up
        case COPY: Files.copy(downloadedFile.toPath(), installPath.resolve(StringUtils.substringAfterLast(descriptor.getOriginalDownloadDescriptor().getDownloadUrl(), "/")), StandardCopyOption.REPLACE_EXISTING); break;
        case EXTRACT_ZIP: FileUtils.extractZip(downloadedFile, installPath); break;
        default: throw new RuntimeException("Unknown install type: "+installType.name());
        }
        downloadedFile.delete();
        postInstall(descriptor);
        updateBinPermissions(Paths.get(descriptor.getBinDir()));
    }

    protected String getInstallDirOrDefault(ToolVersionDownloadDescriptor descriptor) {
        String installDir = getInstallDir();
        if ( StringUtils.isBlank(installDir) ) {
            installDir = FcliHomeHelper.getFortifyHomePath().resolve(String.format("tools/%s/%s", getToolName(), descriptor.getVersion())).toString();
        }
        return installDir;
    }
    
    protected String getBinDir(ToolVersionDownloadDescriptor descriptor) {
        return Paths.get(getInstallDirOrDefault(descriptor), "bin").toString();
    }
    
    protected abstract String getToolName();
    protected abstract InstallType getInstallType();
    protected abstract void postInstall(ToolVersionInstallDescriptor installDescriptor) throws IOException;
    
    private final void emptyExistingInstallPath(Path installPath) throws IOException {
        if ( Files.exists(installPath) && Files.list(installPath).findFirst().isPresent() ) {
            if ( !replaceExisting ) {
                throw new IllegalStateException(String.format("Non-empty installation directory %s already exists; use --replace-existing to replace existing installation", installPath.toString()));
            } else {
                FileUtils.deleteRecursive(installPath);
            }
        }
    }
    
    private final void checkDigest(ToolVersionDownloadDescriptor descriptor, File downloadedFile) {
        String actualDigest = FileUtils.getFileDigest(downloadedFile, descriptor.getDigestAlgorithm());
        String expectedDigest = descriptor.getExpectedDigest();
        if ( !actualDigest.equals(expectedDigest) ) {
            String msg = "Digest mismatch"
                    +"\n Expected: "+expectedDigest
                    +"\n Actual:   "+actualDigest;
            switch(onDigestMismatch) {
            case fail: throw new IllegalStateException(msg);
            case warn: LOG.warn(msg);
            }
        }
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
        EXTRACT_ZIP, COPY
    }
}
