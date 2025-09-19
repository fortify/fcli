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
package com.fortify.cli.sc_sast.scan.cli.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.DebugHelper;
import com.fortify.cli.sc_sast._common.output.cli.cmd.AbstractSCSastJsonNodeOutputCommand;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobHelper;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobHelper.StatusEndpointVersion;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobType;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanPayloadDescriptor;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanPayloadHelper;
import com.fortify.cli.sc_sast.sensor_pool.cli.mixin.SCSastSensorPoolResolverMixin;
import com.fortify.cli.ssc.access_control.helper.SSCTokenConverter;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin.AbstractSSCAppVersionResolverMixin;

import kong.unirest.MultipartBody;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Start.CMD_NAME)
public final class SCSastScanStartCommand extends AbstractSCSastJsonNodeOutputCommand implements IActionCommandResultSupplier {
    // Constants to make sure same option names are defined in Option annotation, 
    // and passed to SCSastScanPayloadHelperBuilder
    private static final String SENSOR_VERSION_OPT_LONG = "--sensor-version";
    private static final String SENSOR_VERSION_OPT_SHORT = "-v";

    @Getter @Mixin private OutputHelperMixins.Start outputHelper;
    
    private String userName = System.getProperty("user.name", "unknown"); // TODO Do we want to give an option to override this?
    @Mixin private SCSastSensorPoolResolverMixin.OptionalOption sensorPoolResolver;
    @Mixin private PublishToAppVersionMixin publishToAppVersionMixin;
    @Mixin private CommonOptionMixins.RequiredFile scanPayloadFileMixin;
    @Option(names = {SENSOR_VERSION_OPT_LONG, SENSOR_VERSION_OPT_SHORT}) private String sensorVersion;
    @Option(names = {"--notify"}) private String email; // TODO Add email address validation
	@Option(names = {"--sargs", "--scan-args"}) private String scanArguments = "";
	@Option(names = {"--no-replace"}) private Boolean noReplace;
	@Option(names = {"--scan-timeout"}) private Integer scanTimeout;
	@Option(names = {"--diagnose"}) private Boolean diagnose;
	
    
    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        String enableDiagnosis = String.valueOf(DebugHelper.isDebugEnabled() || Boolean.TRUE.equals(diagnose));
        var payloadDescriptor = getScanPayloadDescriptor();
        var scanArgsHelper = ScanArgsHelper.parse(scanArguments);
        MultipartBody body = unirest.post("/rest/v2/job")
            .multiPartContent()
            .field("zipFile", createZipFile(payloadDescriptor, scanArgsHelper.getInputFileToZipEntryMap()), "application/zip")
            .field("username", userName, "text/plain")
            .field("scaVersion", payloadDescriptor.getProductVersion(), "text/plain")
            .field("clientVersion", payloadDescriptor.getProductVersion(), "text/plain")
            .field("jobType", payloadDescriptor.getJobType().name(), "text/plain")
            .field("scaRuntimeArgs", scanArgsHelper.getScanArgs(), "text/plain");
        
        body = updateBody(body, "email", email);
        body = updateBody(body, "buildId", payloadDescriptor.getBuildId());
        body = updateBody(body, "pvId", getAppVersionId());
        body = updateBody(body, "poolUuid", getSensorPoolUuid());
        body = updateBody(body, "uploadToken", getUploadToken());
        body = updateBody(body, "dotNetRequired", String.valueOf(payloadDescriptor.isDotNetRequired()));
        body = updateBody(body, "dotNetFrameworkRequiredVersion", payloadDescriptor.getDotNetVersion());
        body = updateBody(body, "requiredOs", payloadDescriptor.getRequiredOs().toString());
        body = updateBody(body, "fprNameOnSsc", publishToAppVersionMixin.getFprFileName());
        body = updateBody(body, "disallowReplacement", noReplace==null ? null : String.valueOf(noReplace));
        body = updateBody(body, "scanTimeout", scanTimeout==null ? null : String.valueOf(scanTimeout));
        body = updateBody(body, "enableDiagnosis", enableDiagnosis);

        JsonNode response = body.asObject(JsonNode.class).getBody();
        if ( !response.has("token") ) {
            throw new FcliSimpleException("Unexpected response when submitting scan job: "+response);
        }
        String scanJobToken = response.get("token").asText();
        return SCSastScanJobHelper.getScanJobDescriptor(unirest, scanJobToken, StatusEndpointVersion.v1).asJsonNode();
    }

	/**
     * @return
     */
    private SCSastScanPayloadDescriptor getScanPayloadDescriptor() {
        return SCSastScanPayloadHelper.builder()
                .payloadFile(scanPayloadFileMixin.getFile())
                .overrideProductVersion(sensorVersion)
                .overrideProductVersionOptionNames(String.format("%s/%s", SENSOR_VERSION_OPT_LONG, SENSOR_VERSION_OPT_SHORT))
                .build().loadDescriptor();
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
        return publishToAppVersionMixin.hasAppVersion()
                ? publishToAppVersionMixin.getAppVersionId(getSscUnirestInstance())
                : null;
    }

    private String getSensorPoolUuid() {
        return sensorPoolResolver.hasValue()
                ? sensorPoolResolver.getSensorPoolUuid(getUnirestInstance())
                : null;
    }
    
    private String getUploadToken() {
        String uploadToken = null;
        if ( !publishToAppVersionMixin.hasAppVersion() ) {
            if ( !StringUtils.isBlank(publishToAppVersionMixin.publishToken) ) {
                throw new FcliSimpleException("Option --publish-token may only be specified if --publish-to has been specified");
            }
        } else {
        	if ( !StringUtils.isBlank(publishToAppVersionMixin.publishToken) ) {
        		// Convert token to application token, in case it was provided as a REST token
        		uploadToken = SSCTokenConverter.toApplicationToken(publishToAppVersionMixin.publishToken);
        	} else {
                char[] tokenFromSession = getUnirestInstanceSupplier().getSessionDescriptor().getActiveSSCToken();
                uploadToken = tokenFromSession==null ? null : SSCTokenConverter.toApplicationToken(String.valueOf(tokenFromSession));
            }
            if ( StringUtils.isBlank(uploadToken) ) { throw new FcliBugException("No publish token provided, and no token available from session"); }
        }
        return uploadToken;
    }
    
    private final MultipartBody updateBody(MultipartBody body, String field, String value) {
        return StringUtils.isBlank(value) ? body : body.field(field, value, "text/plain");
    }
    
    private File createZipFile(SCSastScanPayloadDescriptor payloadDescriptor, Map<File, String> extraFiles) {
        try {
            File zipFile = File.createTempFile("zip", ".zip");
            zipFile.deleteOnExit();
            try (FileOutputStream fout = new FileOutputStream(zipFile); ZipOutputStream zout = new ZipOutputStream(fout)) {
                final String fileName = (payloadDescriptor.getJobType() == SCSastScanJobType.TRANSLATION_AND_SCAN_JOB) ? "translation.zip" : "session.mbs";
                addFile( zout, fileName, payloadDescriptor.getPayloadFile());
                
                for (var extraFile : extraFiles.entrySet() ) {
                	addFile(zout, extraFile.getValue(), extraFile.getKey());
				}
            }
            return zipFile;
        } catch (IOException e) {
            throw new FcliSimpleException("Error creating job file", e);
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

    private static final class PublishToAppVersionMixin extends AbstractSSCAppVersionResolverMixin {
        @Option(names = {"--publish-to"}, required = false)
        @Getter private String appVersionNameOrId;
        @Option(names = {"--publish-as"}, required = false)
        @Getter private String fprFileName = "";
        @Option(names = {"--publish-token"})
        @MaskValue(sensitivity = LogSensitivityLevel.high, description = "SC-SAST PUBLISH TOKEN")
        private String publishToken;
        public final boolean hasAppVersion() { return StringUtils.isNotBlank(appVersionNameOrId); }
    }
    
    @RequiredArgsConstructor
    private static final class ScanArgsHelper {
        @Getter private final String scanArgs;
        @Getter private final Map<File, String> inputFileToZipEntryMap;
        
        public static final ScanArgsHelper parse(String scanArgs) {
            List<String> newArgs = new ArrayList<>();
            Map<File, String> inputFileToZipEntryMap = new LinkedHashMap<>();
            String[] parts = scanArgs.split(" (?=(?:[^\']*\'[^\']*\')*[^\']*$)");
            for ( var part: parts ) {
                var inputFileName = getInputFileName(part);
                if ( inputFileName==null ) {
                    newArgs.add(part.replace("'", "\""));
                } else {
                    var inputFile = new File(inputFileName);
                    if ( !inputFile.canRead() ) {
                        throw new FcliSimpleException("Can't read file "+inputFileName+" as specified in --sargs");
                    }
                    // Re-use existing zip entry name if same file was processed before
                    var zipEntryFileName = inputFileToZipEntryMap.getOrDefault(inputFile, getZipEntryFileName(inputFileName));
                    newArgs.add("\""+zipEntryFileName+"\"");
                    inputFileToZipEntryMap.put(inputFile, zipEntryFileName);
                }
            }
            return new ScanArgsHelper(String.join(" ", newArgs), inputFileToZipEntryMap);
        }
        
        private static final String getInputFileName(String part) {
            var pattern = Pattern.compile("^'?@'?([^\']*)'?$");
            var matcher = pattern.matcher(part);
            return matcher.matches() ? matcher.group(1) : null;
        }
        
        private static final String getZipEntryFileName(String orgFileName) {
            return orgFileName.replaceAll("[^A-Za-z0-9.]", "_");
        }
    }
}