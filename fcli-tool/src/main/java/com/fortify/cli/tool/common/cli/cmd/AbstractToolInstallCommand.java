package com.fortify.cli.tool.common.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.util.FcliHomeHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool.common.helper.ToolHelper;
import com.fortify.cli.tool.common.helper.ToolInstallDescriptor.ToolVersionInstallDescriptor;
import com.fortify.cli.tool.util.FileUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.Unirest;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
public abstract class AbstractToolInstallCommand extends AbstractBasicOutputCommand {
    private static final Set<PosixFilePermission> binPermissions = PosixFilePermissions.fromString("rwxr-xr-x");
    @Getter @Mixin private BasicOutputHelperMixins.Install outputHelper;
    @Getter @Parameters(index="0", arity="0..1") private String version;
    @Getter @Option(names={"-d", "--install-dir"}, required = false) private String installDir;
    @Getter @Option(names={"-y", "--replace-existing"}, required = false) private boolean replaceExisting;
    
    @Override
    protected final JsonNode getJsonNode() {
        String toolName = getToolName();
        ToolVersionInstallDescriptor descriptor = ToolHelper.getToolInstallDescriptor(toolName).getVersionOrDefault(version);
        return downloadAndInstall(descriptor);
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private final JsonNode downloadAndInstall(ToolVersionInstallDescriptor descriptor) {
        try {
            Path installPath = Paths.get(getInstallDirOrDefault(descriptor)).toAbsolutePath();
            Path binPath = Paths.get(getBinDir(descriptor)).toAbsolutePath();
            emptyExistingInstallPath(installPath);
            File downloadedFile = download(descriptor);
            checkDigest(descriptor, downloadedFile);
            install(descriptor, installPath, binPath, downloadedFile);
            return new ObjectMapper().createObjectNode()
                    .put("name", getToolName())
                    .put("version", descriptor.getVersionName())
                    .put("installDir", installPath.toString())
                    .put("binDir", Files.exists(binPath) ? binPath.toString() : "");
            
        } catch ( IOException e ) {
            throw new RuntimeException("Error installing "+getToolName(), e);
        }
    }

    private final File download(ToolVersionInstallDescriptor descriptor) throws IOException {
        File tempDownloadFile = File.createTempFile("fcli-tool-download", null);
        tempDownloadFile.deleteOnExit();
        Unirest.get(descriptor.getDownloadUrl())
            .asFile(tempDownloadFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
            .getBody();
        return tempDownloadFile;
    }
    
    protected void install(ToolVersionInstallDescriptor descriptor, Path installPath, Path binPath, File downloadedFile) throws IOException {
        Files.createDirectories(installPath);
        InstallType installType = getInstallType();
        switch (installType) {
        // TODO Clean this up
        case COPY: Files.copy(downloadedFile.toPath(), installPath.resolve(StringUtils.substringAfterLast(descriptor.getDownloadUrl(), "/")), StandardCopyOption.REPLACE_EXISTING); break;
        case EXTRACT_ZIP: FileUtils.extractZip(downloadedFile, installPath); break;
        default: throw new RuntimeException("Unknown install type: "+installType.name());
        }
        downloadedFile.delete();
        postInstall(descriptor, installPath, binPath);
        updateBinPermissions(binPath);
    }

    protected String getInstallDirOrDefault(ToolVersionInstallDescriptor descriptor) {
        String installDir = getInstallDir();
        if ( StringUtils.isBlank(installDir) ) {
            installDir = FcliHomeHelper.getFortifyHomePath().resolve(String.format("tools/%s/%s", getToolName(), descriptor.getVersionName())).toString();
        }
        return installDir;
    }
    
    protected String getBinDir(ToolVersionInstallDescriptor descriptor) {
        return Paths.get(getInstallDirOrDefault(descriptor), "bin").toString();
    }
    
    protected abstract String getToolName();
    protected abstract InstallType getInstallType();
    protected abstract void postInstall(ToolVersionInstallDescriptor descriptor, Path installPath, Path binPath) throws IOException;
    
    private final void emptyExistingInstallPath(Path installPath) throws IOException {
        if ( Files.exists(installPath) && Files.list(installPath).findFirst().isPresent() ) {
            if ( !replaceExisting ) {
                throw new IllegalStateException(String.format("Non-empty installation directory %s already exists; use --replace-existing to replace existing installation", installPath.toString()));
            } else {
                try (Stream<Path> walk = Files.walk(installPath)) {
                    walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                }
            }
        }
    }
    
    private static final void checkDigest(ToolVersionInstallDescriptor descriptor, File downloadedFile) {
        String actualDigest = FileUtils.getFileDigest(downloadedFile, descriptor.getDigestAlgorithm());
        String expectedDigest = descriptor.getExpectedDigest();
        if ( !actualDigest.equals(expectedDigest) ) {
            throw new IllegalStateException("Digest mismatch"
                    +"\n Expected: "+expectedDigest
                    +"\n Actual:   "+actualDigest);
        }
    }
    
    private static final void updateBinPermissions(Path binPath) throws IOException {
        try (Stream<Path> walk = Files.walk(binPath)) {
            walk.forEach(AbstractToolInstallCommand::updateFilePermissions);
        }
    }
        
    @SneakyThrows
    private static final void updateFilePermissions(Path p) {
        Files.setPosixFilePermissions(p, binPermissions);
    }
    
    protected static enum InstallType {
        EXTRACT_ZIP, COPY
    }
}
