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
package com.fortify.cli.debricked._common.repo.helper;

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.debricked._common.session.helper.DebrickedAuthHelper;
import com.fortify.cli.debricked._common.session.helper.IDebrickedLoginOptions;

import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
public final class DebrickedNoSessionRepoHelper  {
	private IDebrickedLoginOptions loginOptions;
	private String repository;
	private String branch;

	public DebrickedNoSessionRepoHelper(IDebrickedLoginOptions loginOptions, String repository, String branch) {
		this.loginOptions = loginOptions;
		this.repository = repository;
		this.branch = branch;
	}

    public final void downloadSbom(UnirestInstance debrickedUnirest, File file) {
    	DebrickedAuthHelper.configureAuthenticatedUnirest(debrickedUnirest, loginOptions);
    	String reportUuid = startSbomGeneration(debrickedUnirest);
    	waitSbomGeneration(debrickedUnirest, reportUuid, file);
    }
	
	public final String getRepositoryId(UnirestInstance debrickedUnirest) {
		try {
			Integer.parseInt(repository);
			return repository;
		} catch ( NumberFormatException e ) {
			ArrayNode data = debrickedUnirest.get("/api/1.0/open/repositories/get-repositories-names-links")
				.asObject(ArrayNode.class)
				.getBody();
			// TODO Improve this to properly handle generics
			// TODO Get rid of appending empty string to id to convert int to string as expected by SpEL
			@SuppressWarnings("unchecked")
            List<String> repositoryIds = JsonHelper.evaluateSpelExpression(data, "?[name == '"+repository+"'].![id+'']", ArrayList.class);
			switch ( repositoryIds.size() ) {
				case 0: throw new FcliSimpleException(String.format("Debricked repository with name %s not found; please use full repository name like <org>/<repo>", repository));
				case 1: return repositoryIds.get(0);
				default: throw new FcliSimpleException(String.format("Multiple debricked repositories with name %s found; please use repository id instead", repository));
			}
		}
	}
	
	public final String startSbomGeneration(UnirestInstance debrickedUnirest) {
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
	public final void waitSbomGeneration(UnirestInstance debrickedUnirest, String reportUuid, File outputFile) {
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
