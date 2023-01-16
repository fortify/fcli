/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.appversion_artifact.cli.cmd.imprt.debricked;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.runner.GenericUnirestRunner;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.AbstractSSCAppVersionArtifactUploadCommand;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.imprt.debricked.DebrickedLoginOptions.DebrickedAccessTokenCredentialOptions;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.imprt.debricked.DebrickedLoginOptions.DebrickedAuthOptions;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.imprt.debricked.DebrickedLoginOptions.DebrickedUserCredentialOptions;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess @FixInjection
@Command(name = SSCOutputHelperMixins.ImportFromDebricked.CMD_NAME)
public class SSCAppVersionArtifactImportFromDebrickedCommand extends AbstractSSCAppVersionArtifactUploadCommand {
    @Getter @Mixin private SSCOutputHelperMixins.ImportFromDebricked outputHelper;
    @Mixin private DebrickedLoginOptions debrickedLoginOptions; 
    @Inject private GenericUnirestRunner debrickedUnirestRunner;
    
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
    protected void preUpload(UnirestInstance unirest, File file) {
    	debrickedUnirestRunner.run(u->downloadSbom(u, file));
    }
    
    @Override
    protected void postUpload(UnirestInstance unirest, File file) {
    	if ( StringUtils.isBlank(fileName) ) {
    		file.delete();
    	}
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
        String debrickedJwtToken = getDebrickedJwtToken(debrickedUnirest);
        UnirestJsonHeaderConfigurer.configure(debrickedUnirest);
		String authHeader = String.format("Bearer %s", debrickedJwtToken);
        debrickedUnirest.config().addDefaultHeader("Authorization", authHeader);
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
			JsonNode data = debrickedUnirest.get("/api/1.0/open/repositories/get-repositories-names-links")
				.asObject(JsonNode.class)
				.getBody();
			return JsonHelper.evaluateJsonPath(data, "$[?(@.name == \""+repository+"\")].id", String.class);
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
			// Work-around for debricked not returning proper HTTP status code
			status = status!=200 ? status : checkResponse(outputFile);
		}
	}

	@SneakyThrows
	private int checkResponse(File file) {
		Pattern p = Pattern.compile(".*\\\"statusCode\\\":([\\d]+).*");
		Path path = file.toPath();
		try ( Stream<String> lines = Files.lines(path) ) {
			String statusCode = lines.map(p::matcher)
		     	.filter(Matcher::matches)
		     	.findFirst()
		     	.map(m->m.group(1))
		     	.orElse(null);
			int status = statusCode==null ? 200 : Integer.parseInt(statusCode);
			if ( status!=200 && status!=202 ) {
				throw new IllegalStateException("Unexpected Debricked response: "+Files.readString(path));
			}
			return status;
		}
	}
}
