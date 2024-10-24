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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.sc_sast._common.output.cli.cmd.AbstractSCSastControllerJsonNodeOutputCommand;
import com.fortify.cli.sc_sast.scan.cli.mixin.SCSastScanStartOptionsArgGroup;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerJobType;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobHelper;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobHelper.StatusEndpointVersion;
import com.fortify.cli.sc_sast.sensor_pool.cli.mixin.SCSastSensorPoolResolverMixin;
import com.fortify.cli.ssc.access_control.helper.SSCTokenConverter;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin.AbstractSSCAppVersionResolverMixin;

import kong.unirest.MultipartBody;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
    @Mixin private SCSastSensorPoolResolverMixin.OptionalOption sensorPoolResolver;
    @Mixin private PublishToAppVersionResolverMixin sscAppVersionResolver;
    @Option(names = "--ssc-ci-token") private String ciToken;
	@Option(names = { "--sargs", "--scan-args" })
	private String scanArguments = "";
    
    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        String sensorVersion = normalizeSensorVersion(optionsProvider.getScanStartOptions().getSensorVersion());
        var scanArgsHelper = ScanArgsHelper.parse(scanArguments);
        MultipartBody body = unirest.post("/rest/v2/job")
            .multiPartContent()
            .field("zipFile", createZipFile(scanArgsHelper.getInputFileToZipEntryMap()), "application/zip")
            .field("username", userName, "text/plain")
            .field("scaVersion", sensorVersion, "text/plain")
            .field("clientVersion", sensorVersion, "text/plain")
            .field("jobType", optionsProvider.getScanStartOptions().getJobType().name(), "text/plain")
            .field("scaRuntimeArgs", scanArgsHelper.getScanArgs(), "text/plain");
        
        body = updateBody(body, "email", email);
        body = updateBody(body, "buildId", optionsProvider.getScanStartOptions().getBuildId());
        body = updateBody(body, "pvId", getAppVersionId());
        body = updateBody(body, "poolUuid", getSensorPoolUuid());
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

    private String getSensorPoolUuid() {
        return sensorPoolResolver.hasValue()
                ? sensorPoolResolver.getSensorPoolUuid(getUnirestInstance())
                : null;
    }
    
    private String getUploadToken() {
        String uploadToken = null;
        if ( !sscAppVersionResolver.hasValue() ) {
            if ( !StringUtils.isBlank(this.ciToken) ) {
                throw new IllegalArgumentException("Option --ssc-ci-token may only be specified if --publish-to has been specified");
            }
        } else {
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
                char[] ciTokenFromSession = getUnirestInstanceSupplier().getSessionDescriptor().getPredefinedSscToken();
                uploadToken = ciTokenFromSession==null ? null : SSCTokenConverter.toApplicationToken(String.valueOf(ciTokenFromSession));
            }
            if ( StringUtils.isBlank(uploadToken) ) { throw new IllegalArgumentException("--ssc-ci-token is required if --publish-to is specified and --ssc-ci-token was not passed to the 'sc-sast session login' command"); }
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
    
    private File createZipFile(Map<File, String> extraFiles) {
        try {
            File zipFile = File.createTempFile("zip", ".zip");
            zipFile.deleteOnExit();
            try (FileOutputStream fout = new FileOutputStream(zipFile); ZipOutputStream zout = new ZipOutputStream(fout)) {
                final String fileName = (optionsProvider.getScanStartOptions().getJobType() == SCSastControllerJobType.TRANSLATION_AND_SCAN_JOB) ? "translation.zip" : "session.mbs";
                addFile( zout, fileName, optionsProvider.getScanStartOptions().getPayloadFile());
                
                for (var extraFile : extraFiles.entrySet() ) {
                	addFile(zout, extraFile.getValue(), extraFile.getKey());
				}
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

    private static final class PublishToAppVersionResolverMixin extends AbstractSSCAppVersionResolverMixin {
        @Option(names = {"--publish-to"}, required = false)
        @Getter private String appVersionNameOrId;
        public final boolean hasValue() { return StringUtils.isNotBlank(appVersionNameOrId); }
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
                        throw new IllegalArgumentException("Can't read file "+inputFileName+" as specified in --sargs");
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
            var pattern = Pattern.compile("^'?file:'?([^\']*)'?$");
            var matcher = pattern.matcher(part);
            return matcher.matches() ? matcher.group(1) : null;
        }
        
        private static final String getZipEntryFileName(String orgFileName) {
            return orgFileName.replaceAll("[^A-Za-z0-9.]", "_");
        }
    }
}