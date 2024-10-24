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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
	@Option(names = { "--sargs", "--scan-args" }, description = "Runtime scan arguments to Source Analyzer.")
	private String scanArguments = "";
	
	private Map<String, Set<String>> scanFileArgs = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> compressedFilesMap = new HashMap<String, Set<String>>();
	private Set<ScanArgument> scanArgumentsSet;

    // TODO Add options for specifying (custom) rules file(s), filter file(s) and project template
    // TODO Add options for pool selection
    
    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        String sensorVersion = normalizeSensorVersion(optionsProvider.getScanStartOptions().getSensorVersion());
        
        processScanArguments();
        MultipartBody body = unirest.post("/rest/v2/job")
            .multiPartContent()
            .field("zipFile", createZipFile(), "application/zip")
            .field("username", userName, "text/plain")
            .field("scaVersion", sensorVersion, "text/plain")
            .field("clientVersion", sensorVersion, "text/plain")
            .field("jobType", optionsProvider.getScanStartOptions().getJobType().name(), "text/plain")
            .field("scaRuntimeArgs", constructSCAArgs(), "text/plain");
        
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

	private String constructSCAArgs() {
		StringBuffer buffer = new StringBuffer();
		for (ScanArgument scanArgument : scanArgumentsSet) {
			String argKey = scanArgument.getArgKey();
			String argValue = scanArgument.getArgValue();
			boolean fileArgument = scanArgument.isFileArgument();
			if (fileArgument) {
				Set<String> scanArgFiles = compressedFilesMap.get(argKey);
				if (null != scanArgFiles) {
					int size = scanArgFiles.size();
					for (String scanArgumentFileName : scanArgFiles) {
						buffer.append(argKey);
						buffer.append(" \'");
						buffer.append(scanArgumentFileName);
						buffer.append("\'");
						if (size > 1) {
							buffer.append(" ");
							--size;
						}
					}
				}
				compressedFilesMap.remove(argKey);
			} else {
				buffer.append(argKey);
				if (argValue!=null) {
					buffer.append(" ");
					buffer.append(argValue);
				}
			}
			buffer.append(" ");
		}
		return buffer.toString().trim();
	}
	
	private void processScanArguments() {
		scanArgumentsSet = processScanRuntimeArgs();
		for (ScanArgument scanArgument : scanArgumentsSet) {
			if (scanArgument.isFileArgument()) {
				String key = scanArgument.getArgKey();
				String value = scanArgument.getArgValue();
				scanFileArgs.computeIfAbsent(key, k -> new HashSet<>()).add(value);
			}
		}
		updateFileNamesToUniqueName();
	}

	private Set<ScanArgument> processScanRuntimeArgs() {
		Set<ScanArgument> scanArgsSet = new HashSet<ScanArgument>();
		String[] parts = scanArguments.split(" (?=(?:[^\']*\'[^\']*\')*[^\']*$)");

		ScanArgument scanArgument = null;
		for (String part : parts) {
			String key = null;
			String value = null;
			boolean isFileArg = false;
			
			if (part.startsWith("-")) {
				key = part.trim();
				scanArgument = new ScanArgument();
				scanArgument.setArgKey(key);
			} else {
				if (part.startsWith("file:")) {
					isFileArg = true;
				}
				value = part.replace("file:", "").replace("'", "");
				scanArgument.setArgValue(value);
				scanArgument.setFileArgument(isFileArg);
			}
			scanArgsSet.add(scanArgument);
		}
		return scanArgsSet;
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
    
    private File createZipFile() {
        try {
            File zipFile = File.createTempFile("zip", ".zip");
            zipFile.deleteOnExit();
            try (FileOutputStream fout = new FileOutputStream(zipFile); ZipOutputStream zout = new ZipOutputStream(fout)) {
                final String fileName = (optionsProvider.getScanStartOptions().getJobType() == SCSastControllerJobType.TRANSLATION_AND_SCAN_JOB) ? "translation.zip" : "session.mbs";
                addFile( zout, fileName, optionsProvider.getScanStartOptions().getPayloadFile());
                
                for (Entry<String, Set<String>> fileArgsMap : compressedFilesMap.entrySet()) {
                	Set<String> files = fileArgsMap.getValue();
                	for (String file : files) {
                		addFile(zout, file, optionsProvider.getScanStartOptions().getPayloadFile());
                	}
				}
            }
            return zipFile;
        } catch (IOException e) {
            throw new RuntimeException("Error creating job file", e);
        }
    }

	private void updateFileNamesToUniqueName() {
		for (Entry<String, Set<String>> fileArg : scanFileArgs.entrySet()) {
			String argName = fileArg.getKey();
			Set<String> argValues = fileArg.getValue();
			Set<String> compressedFileNames = new HashSet<String>();
			for (String argValue : argValues) {
				String uniqueFileName = constructUniqueFileName(argValue);
				compressedFileNames.add(uniqueFileName);
				
			}
			compressedFilesMap.put(argName, compressedFileNames);
		}
	}

	private String constructUniqueFileName(String argValue) {
		return argValue.replaceAll("[^A-Za-z0-9.]", "_");
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
}


class ScanArgument {
	private boolean isFileArgument;
	private String argKey;
	private String argValue;

	public void setFileArgument(boolean isFileArgument) {
		this.isFileArgument = isFileArgument;
	}

	public void setArgKey(String argKey) {
		this.argKey = argKey;
	}

	public void setArgValue(String argValue) {
		this.argValue = argValue;
	}

	public boolean isFileArgument() {
		return isFileArgument;
	}

	public String getArgKey() {
		return argKey;
	}

	public String getArgValue() {
		return argValue;
	}
	
	@Override
	public String toString() {
		return "Argument " + argKey + (isFileArgument? " is a file argument with value " : " is not a file argument with value ") + argValue;
	}
}