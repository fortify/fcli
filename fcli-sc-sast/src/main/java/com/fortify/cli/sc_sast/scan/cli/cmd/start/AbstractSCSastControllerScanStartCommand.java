package com.fortify.cli.sc_sast.scan.cli.cmd.start;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.sc_sast.output.cli.cmd.AbstractSCSastControllerOutputCommand;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerJobType;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobHelper;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobHelper.StatusEndpointVersion;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.MultipartBody;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
public abstract class AbstractSCSastControllerScanStartCommand extends AbstractSCSastControllerOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier {
    private String userName = System.getProperty("user.name", "unknown"); // TODO Do we want to give an option to override this?
    @Option(names = "--notify") private String email; // TODO Add email address validation
    @Mixin private SSCAppVersionResolverMixin.OptionalOption sscAppVersionResolver;
    @Option(names = "--no-upload", negatable = true) private boolean upload = true;
    @Option(names = "--ci-token") private String ciToken;
    
    // TODO Add options for specifying (custom) rules file(s), filter file(s) and project template
    // TODO Add options for pool selection
    
    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        String sensorVersion = normalizeSensorVersion(getSensorVersion());
        MultipartBody body = unirest.post("/rest/v2/job")
            .multiPartContent()
            .field("zipFile", createZipFile(), "application/zip")
            .field("username", userName, "text/plain")
            .field("scaVersion", sensorVersion, "text/plain")
            .field("clientVersion", sensorVersion, "text/plain")
            .field("scaRuntimeArgs", getScaRuntimeArgs(), "text/plain")
            .field("jobType", getJobType().name(), "text/plain");
        body = updateBody(body, "email", email);
        body = updateBody(body, "buildId", getBuildId());
        body = updateBody(body, "pvId", getAppVersionId());
        body = updateBody(body, "uploadToken", getUploadToken());
        body = updateBody(body, "dotNetRequired", String.valueOf(isDotNetRequired()));
        body = updateBody(body, "dotNetFrameworkRequiredVersion", getDotNetVersion());
        JsonNode response = body.asObject(JsonNode.class).getBody();
        if ( !response.has("token") ) {
            throw new IllegalStateException("Unexpected response when submitting scan job: "+response);
        }
        String scanJobToken = response.get("token").asText();
        return SCSastControllerScanJobHelper.getScanJobDescriptor(unirest, scanJobToken, StatusEndpointVersion.v1).asJsonNode();
    }

    @Override
    public final String getActionCommandResult() {
        return "SCAN_REQUESTED";
    }
    
    @Override
    public final boolean isSingular() {
        return true;
    }
    
    protected abstract String getBuildId();
    protected abstract String getScaRuntimeArgs();
    protected abstract boolean isDotNetRequired();
    protected abstract String getDotNetVersion();
    protected abstract File getPayloadFile();
    protected abstract String getSensorVersion();
    protected abstract SCSastControllerJobType getJobType();
    
    private String getAppVersionId() {
        return sscAppVersionResolver.hasValue()
            ? runOnSSC(sscAppVersionResolver::getAppVersionId)
            : null;
    }
    
    private String getUploadToken() {
        String uploadToken = null;
        if ( upload ) {
            uploadToken = this.ciToken;
            // TODO Get ciToken from session if not specified
            if ( StringUtils.isBlank(uploadToken) ) { throw new IllegalArgumentException("--ci-token is required unless --no-upload is specified"); }
        }
        return uploadToken;
    }
    
    private String normalizeSensorVersion(String sensorVersion) {
        return sensorVersion.chars().filter(ch -> ch == '.').count()==1
                ? sensorVersion+".0"
                : sensorVersion;
    }
    
    private final MultipartBody updateBody(MultipartBody body, String field, String value) {
        return StringUtils.isBlank(value) ? body : body.field(field, value, "text/plain");
    }
    
    private File createZipFile() {
        try {
            File zipFile = File.createTempFile("zip", ".zip");
            zipFile.deleteOnExit();
            try (FileOutputStream fout = new FileOutputStream(zipFile); ZipOutputStream zout = new ZipOutputStream(fout)) {
                final String fileName = (getJobType() == SCSastControllerJobType.TRANSLATION_AND_SCAN_JOB) ? "translation.zip" : "session.mbs";
                addFile( zout, fileName, getPayloadFile());
                // TODO Add rule files, filter files, issue template
            }
            return zipFile;
        } catch (IOException e) {
            throw new RuntimeException("Error creating job file", e);
        }
    }

    private void addFile(ZipOutputStream zout, String fileName, File file) throws IOException {
        try ( FileInputStream in = new FileInputStream(file)) {
            zout.putNextEntry(new ZipEntry(fileName));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                zout.write(buffer, 0, len);
            }
            zout.closeEntry();
        }
    }
}
