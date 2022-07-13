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

import com.fortify.cli.ssc.picocli.command.SSCUrls;
import com.jayway.jsonpath.JsonPath;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import javax.validation.ValidationException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@ReflectiveAccess
public class SSCApplicationVersionIdMixin {
	
	public static abstract class AbstractSSCApplicationVersionMixin {
		public abstract String getVersionNameOrId();

		@SneakyThrows
		private String encodeValue(String value) {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		}

		@Option(names = {"--delim"},
				description = "Change the default delimiter character when using options that accepts " +
				"\"application:version\" as an argument or parameter.", defaultValue = ":")
		private String delimiter;
		
		public String getApplicationVersionId(UnirestInstance unirestInstance) {
			String versionNameOrId = getVersionNameOrId();

			if(getVersionNameOrId().contains(delimiter)){
				String[] app = getVersionNameOrId().split(delimiter);
				String searchQuery = "?limit=-1&fields=id,name,project&q=" + encodeValue(String.format("project.name:\"%s\",name:\"%s\"", app[0], app[1]));
				String response = unirestInstance.get(SSCUrls.PROJECT_VERSIONS + searchQuery).getBody().toString();
				return JsonPath.parse(response).read("$.data[0].id").toString();
			}

			if(Integer.parseInt(getVersionNameOrId()) <= -1)
				throw new ValidationException("The provided Application Version ID is not valid.");
			return versionNameOrId;
		}
	}
	
	// get/retrieve/delete/download version <entity> --from
	public static class From extends AbstractSSCApplicationVersionMixin {
		@Option(names = {"--from"}, required = true, description = "Application version id or <application>:<version> name")
		@Getter private String versionNameOrId;
	}
	
	// create/update version <entity> --for <version>
	public static class For extends AbstractSSCApplicationVersionMixin {
		@Option(names = {"--for"}, required = true, description = "Application version id or <application>:<version> name")
		@Getter private String versionNameOrId;
	}
	
	// upload version <entity> --to <version>
	public static class To extends AbstractSSCApplicationVersionMixin {
		@Option(names = {"--to"}, required = true, description = "Application version id or <application>:<version> name")
		@Getter private String versionNameOrId;
	}
	
	// delete|update <versionNameOrId>
	public static class PositionalParameter extends AbstractSSCApplicationVersionMixin {
		@Parameters(index = "0", arity = "1", description = "Application version id or <application>:<version> name")
		@Getter private String versionNameOrId;
	}
}
