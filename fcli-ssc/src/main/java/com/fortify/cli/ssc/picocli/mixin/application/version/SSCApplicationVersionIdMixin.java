/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.picocli.mixin.application.version;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.ssc.domain.version.ApplicationVersion;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.jayway.jsonpath.JsonPath;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import javax.validation.ValidationException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@ReflectiveAccess
public class SSCApplicationVersionIdMixin {
	
	public static abstract class AbstractSSCApplicationVersionMixin {
		public abstract String getVersionNameOrId();

		@SneakyThrows
		public static String urlEncodeValue(String value) {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		}

		@Option(names = {"--delim"},
				description = "Change the default delimiter character when using options that accepts " +
				"\"application:version\" as an argument or parameter.", defaultValue = ":")
		private String delimiter;

		public ApplicationVersion getApplicationAndVersion(UnirestInstance unirestInstance){
			String versionNameOrId = getVersionNameOrId();
			ApplicationVersion av = new ApplicationVersion();

			if(versionNameOrId.contains(delimiter)){
				String[] app = versionNameOrId.split(delimiter);
				String searchQuery = "?limit=1&fields=id,name,project&q=" + urlEncodeValue(String.format("project.name:\"%s\",name:\"%s\"", app[0], app[1]));
				HttpResponse response = unirestInstance.get(SSCUrls.PROJECT_VERSIONS + searchQuery).asObject(ObjectNode.class);
				String responseBodyJson = response.getBody().toString();
				av.setApplicationName(app[0]);
				av.setApplicationId(JsonPath.parse(responseBodyJson).read("$.data[0].project.id").toString());
				av.setApplicationVersionName(app[1]);
				av.setApplicationVersionId(JsonPath.parse(responseBodyJson).read("$.data[0].id").toString());
				return av;
			}

			if(Integer.parseInt(versionNameOrId) >= 0){
				String searchQuery = "?fields=id,name,project";
				HttpResponse response = unirestInstance.get(SSCUrls.PROJECT_VERSION(versionNameOrId) + searchQuery).asObject(ObjectNode.class);
				String responseBodyJson = response.getBody().toString();
				av.setApplicationId(JsonPath.parse(responseBodyJson).read("$.data.project.id").toString());
				av.setApplicationName(JsonPath.parse(responseBodyJson).read("$.data.project.name").toString());
				av.setApplicationVersionId(JsonPath.parse(responseBodyJson).read("$.data.id").toString());
				av.setApplicationVersionName(JsonPath.parse(responseBodyJson).read("$.data.name").toString());
				return av;
			}else {
				throw new ValidationException("The provided Application Version ID is not valid.");
			}
		}
		public String getApplicationVersionId(UnirestInstance unirestInstance) {
			return getApplicationAndVersion(unirestInstance).getApplicationVersionId();
		}
	}
	
	// get/retrieve/delete/download version <entity> --from
	public static class From extends AbstractSSCApplicationVersionMixin {
		@Option(names = {"--from"}, required = true, descriptionKey = "ApplicationVersionMixin")
		@Getter private String versionNameOrId;
	}
	
	// create/update version <entity> --for <version>
	public static class For extends AbstractSSCApplicationVersionMixin {
		@Option(names = {"--for"}, required = true, descriptionKey = "ApplicationVersionMixin")
		@Getter private String versionNameOrId;
	}
	
	// upload version <entity> --to <version>
	public static class To extends AbstractSSCApplicationVersionMixin {
		@Option(names = {"--to"}, required = true, descriptionKey = "ApplicationVersionMixin")
		@Getter private String versionNameOrId;
	}
	
	// delete|update <versionNameOrId>
	public static class PositionalParameter extends AbstractSSCApplicationVersionMixin {
		@Parameters(index = "0", arity = "1", descriptionKey = "ApplicationVersionMixin")
		@Getter private String versionNameOrId;
	}
}
