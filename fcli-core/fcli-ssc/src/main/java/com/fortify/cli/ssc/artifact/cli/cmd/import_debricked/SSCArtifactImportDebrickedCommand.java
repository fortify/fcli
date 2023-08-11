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
package com.fortify.cli.ssc.artifact.cli.cmd.import_debricked;

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.fortify.cli.common.http.connection.helper.ConnectionHelper;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.artifact.cli.cmd.AbstractSSCArtifactUploadCommand;
import com.fortify.cli.ssc.artifact.cli.cmd.import_debricked.DebrickedLoginOptions.DebrickedAccessTokenCredentialOptions;
import com.fortify.cli.ssc.artifact.cli.cmd.import_debricked.DebrickedLoginOptions.DebrickedAuthOptions;
import com.fortify.cli.ssc.artifact.cli.cmd.import_debricked.DebrickedLoginOptions.DebrickedUserCredentialOptions;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SSCOutputHelperMixins.ImportDebricked.CMD_NAME)
public class SSCArtifactImportDebrickedCommand extends AbstractSSCArtifactUploadCommand {
    @Mixin @Getter private SSCOutputHelperMixins.ImportDebricked outputHelper;
    @Mixin private DebrickedLoginOptions debrickedLoginOptions; 
    
    @Option(names = {"-e", "--engine-type"}, required = true, defaultValue = "DEBRICKED")
    @Getter private String engineType;
    
    @Option(names = {"-f", "--save-sbom-as"}, required = false)
    private String fileName;
    
    @Option(names = {"-r", "--repository"}, required = true)
    private String repository;
    
    @Option(names = {"-b", "--branch"}, required = true)
    private String branch;
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override @SneakyThrows
    protected File getFile() {
    	File sbomFile = null;
    	if ( StringUtils.isNotBlank(fileName) ) {
    		sbomFile = new File(fileName);
    	} else {
    		sbomFile = File.createTempFile("debricked", ".json");
    		sbomFile.deleteOnExit();
    	}
    	return sbomFile;
    }
    
    @Override
    protected void preUpload(UnirestInstance unirest, IProgressWriterI18n progressWriter, File file) {
    	progressWriter.writeProgress("Status: Generating & downloading SBOM");
    	try ( var debrickedUnirest = GenericUnirestFactory.createUnirestInstance() ) {
    	    downloadSbom(debrickedUnirest, file);
    	}
    	progressWriter.writeProgress("Status: Uploading SBOM to SSC");
    }
    
    @Override
    protected void postUpload(UnirestInstance unirest, IProgressWriterI18n progressWriter, File file) {
    	if ( StringUtils.isBlank(fileName) ) {
    		file.delete();
    	}
    	progressWriter.writeProgress("Status: SBOM uploaded to SSC");
    	progressWriter.clearProgress();
    }
    
    private Void downloadSbom(UnirestInstance debrickedUnirest, File file) {
    	configureDebrickedUnirest(debrickedUnirest);
    	String reportUuid = startSbomGeneration(debrickedUnirest);
    	waitSbomGeneration(debrickedUnirest, reportUuid, file);
    	return null;
    }

	private void configureDebrickedUnirest(UnirestInstance debrickedUnirest) {
		UnirestUnexpectedHttpResponseConfigurer.configure(debrickedUnirest);
        DebrickedUrlConfigOptions debrickedUrlConfig = debrickedLoginOptions.getUrlConfigOptions();
		UnirestUrlConfigConfigurer.configure(debrickedUnirest, debrickedUrlConfig);
        ProxyHelper.configureProxy(debrickedUnirest, "debricked", debrickedUrlConfig.getUrl());
        ConnectionHelper.configureTimeouts(debrickedUnirest, "debricked");
        String debrickedJwtToken = getDebrickedJwtToken(debrickedUnirest);
        UnirestJsonHeaderConfigurer.configure(debrickedUnirest);
		String authHeader = String.format("Bearer %s", debrickedJwtToken);
        debrickedUnirest.config().setDefaultHeader("Authorization", authHeader);
	}

	private String getDebrickedJwtToken(UnirestInstance debrickedUnirest) {
		DebrickedAuthOptions authOptions = debrickedLoginOptions.getAuthOptions();
		DebrickedUserCredentialOptions userCredentialsOptions = authOptions.getUserCredentialsOptions();
		DebrickedAccessTokenCredentialOptions tokenOptions = authOptions.getTokenOptions();
		if ( userCredentialsOptions!=null && StringUtils.isNotBlank(userCredentialsOptions.getUser()) ) {
			return getDebrickedJwtToken(debrickedUnirest, userCredentialsOptions);
		} else if ( tokenOptions!=null && tokenOptions.getAccessToken()!=null ) {
			return getDebrickedJwtToken(debrickedUnirest, tokenOptions);
		} else {
			throw new IllegalArgumentException("Either Debricked user credentials or access token need to be specified");
		}
	}

	private String getDebrickedJwtToken(UnirestInstance debrickedUnirest, DebrickedAccessTokenCredentialOptions tokenOptions) {
		return debrickedUnirest.post("/api/login_refresh")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.field("refresh_token", new String(tokenOptions.getAccessToken()))
				.asObject(JsonNode.class)
				.getBody()
				.get("token")
				.asText();
	}

	private String getDebrickedJwtToken(UnirestInstance debrickedUnirest, DebrickedUserCredentialOptions userCredentialsOptions) {
		return debrickedUnirest.post("/api/login_check")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.field("_username", userCredentialsOptions.getUser())
				.field("_password", new String(userCredentialsOptions.getPassword()))
				.asObject(JsonNode.class)
				.getBody()
				.get("token")
				.asText();
	}
	
	private String getRepositoryId(UnirestInstance debrickedUnirest) {
		try {
			Integer.parseInt(repository);
			return repository;
		} catch ( NumberFormatException e ) {
			ArrayNode data = debrickedUnirest.get("/api/1.0/open/repositories/get-repositories-names-links")
				.asObject(ArrayNode.class)
				.getBody();
			// TODO Improve this to properly handle generics
			// TODO Get rid of appending empty string to id to convert int to string as expected by SpEL
			List<String> repositoryIds = JsonHelper.evaluateSpelExpression(data, "?[name == '"+repository+"'].![id+'']", ArrayList.class);
			switch ( repositoryIds.size() ) {
				case 0: throw new IllegalArgumentException(String.format("Debricked repository with name %s not found; please use full repository name like <org>/<repo>", repository));
				case 1: return repositoryIds.get(0);
				default: throw new IllegalArgumentException(String.format("Multiple debricked repositories with name %s found; please use repository id instead", repository));
			}
		}
	}
	
	private String startSbomGeneration(UnirestInstance debrickedUnirest) {
		ObjectNode body = new ObjectMapper().createObjectNode()
			// TODO generate a proper ArrayNode
			.putRawValue("repositoryIds", new RawValue("["+getRepositoryId(debrickedUnirest)+"]"))
			.put("branch", branch)
			.put("locale", "en")
			.put("vulnerabilities", true)
			.put("licenses", true)
			.put("sendEmail", false);
		return debrickedUnirest.post("/api/1.0/open/sbom/generate-cyclonedx-sbom")
			.body(body)
			.asObject(JsonNode.class)
			.getBody()
			.get("reportUuid")
			.asText();
	}

	@SneakyThrows
	private void waitSbomGeneration(UnirestInstance debrickedUnirest, String reportUuid, File outputFile) {
		int status = 202;
		while ( status==202 ) {
			Thread.sleep(5000L);
			status = debrickedUnirest.get("/api/1.0/open/sbom/download-generated-cyclonedx-sbom")
				.queryString("reportUuid", reportUuid)
				.asFile(outputFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
				.getStatus();
		}
	}
}
