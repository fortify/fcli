/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_sast.entity.scan.cli.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.sc_sast.entity.scan.cli.mixin.SCSastScanStartOptionsArgGroup;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerJobType;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerScanJobHelper;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerScanJobHelper.StatusEndpointVersion;
import com.fortify.cli.sc_sast.output.cli.cmd.AbstractSCSastControllerJsonNodeOutputCommand;
import com.fortify.cli.ssc.entity.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenConverter;

import kong.unirest.MultipartBody;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Start.CMD_NAME)
public final class SCSastControllerScanStartCommand extends AbstractSCSastControllerJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @ArgGroup(exclusive = true, multiplicity = "1") 
    private SCSastScanStartOptionsArgGroup optionsProvider = new SCSastScanStartOptionsArgGroup();
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;
    private String userName = System.getProperty("user.name", "unknown"); // TODO Do we want to give an option to override this?
    @Option(names = "--notify") private String email; // TODO Add email address validation
    @Mixin private SSCAppVersionResolverMixin.OptionalOption sscAppVersionResolver;
    @Option(names = "--no-upload", negatable = true) private boolean upload = true;
    @Option(names = "--ssc-ci-token") private String ciToken;
    
    // TODO Add options for specifying (custom) rules file(s), filter file(s) and project template
    // TODO Add options for pool selection
    
    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        String sensorVersion = normalizeSensorVersion(optionsProvider.getScanStartOptions().getSensorVersion());
        MultipartBody body = unirest.post("/rest/v2/job")
            .multiPartContent()
            .field("zipFile", createZipFile(), "application/zip")
            .field("username", userName, "text/plain")
            .field("scaVersion", sensorVersion, "text/plain")
            .field("clientVersion", sensorVersion, "text/plain")
            .field("scaRuntimeArgs", optionsProvider.getScanStartOptions().getScaRuntimeArgs(), "text/plain")
            .field("jobType", optionsProvider.getScanStartOptions().getJobType().name(), "text/plain");
        body = updateBody(body, "email", email);
        body = updateBody(body, "buildId", optionsProvider.getScanStartOptions().getBuildId());
        body = updateBody(body, "pvId", getAppVersionId());
        body = updateBody(body, "uploadToken", getUploadToken());
        body = updateBody(body, "dotNetRequired", String.valueOf(optionsProvider.getScanStartOptions().isDotNetRequired()));
        body = updateBody(body, "dotNetFrameworkRequiredVersion", optionsProvider.getScanStartOptions().getDotNetVersion());
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
    
    private String getAppVersionId() {
        return sscAppVersionResolver.hasValue()
            ? sscAppVersionResolver.getAppVersionId(getSscUnirestInstance())
            : null;
    }
    
    private String getUploadToken() {
        String uploadToken = null;
        if ( upload ) {
        	if ( !StringUtils.isBlank(this.ciToken) ) {
        		// Convert token to application token, in case it was provided as a REST token
        		uploadToken = SSCTokenConverter.toApplicationToken(this.ciToken);
        	} else if ( StringUtils.isBlank(uploadToken) ) {
                // We assume that the predefined token from the session is a CIToken as passed through 
                // the --ssc-ci-token option on the login command. If we ever add support for logging 
                // in with arbitrary SSC tokens, we should make sure we can distinguish between CIToken 
                // passed through --ssc-ci-token, and arbitrary token passed through --ssc-token on the 
                // login command; we should only reuse a token passed through the --ssc-ci-token login 
                // option.
                char[] ciTokenFromSession = getProductHelper().getSessionDescriptor().getPredefinedSscToken();
                uploadToken = ciTokenFromSession==null ? null : SSCTokenConverter.toApplicationToken(String.valueOf(ciTokenFromSession));
            }
            if ( StringUtils.isBlank(uploadToken) ) { throw new IllegalArgumentException("--ssc-ci-token is required unless --no-upload is specified or if --ssc-ci-token was passed to the 'sc-sast session login' command"); }
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
                final String fileName = (optionsProvider.getScanStartOptions().getJobType() == SCSastControllerJobType.TRANSLATION_AND_SCAN_JOB) ? "translation.zip" : "session.mbs";
                addFile( zout, fileName, optionsProvider.getScanStartOptions().getPayloadFile());
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
