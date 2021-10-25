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
package com.fortify.cli.dast.rest.unirest;

import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.rest.unirest.UnirestRunner;
import com.fortify.cli.ssc.rest.unirest.SSCUnirestRunner;
import com.jayway.jsonpath.JsonPath;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;

@Singleton @ReflectiveAccess
public class SCDastUnirestRunner {
	@Getter @Inject private UnirestRunner unirestRunner;
	@Getter @Inject private SSCUnirestRunner sscUnirestRunner;
	
	public <R> R runWithUnirest(String sscLoginSessionName, Function<UnirestInstance, R> runner) {
		return sscUnirestRunner.runWithUnirest(sscLoginSessionName, sscUnirest -> {
			String scDastApiUrl = getSCDastApiUrlFromSSC(sscUnirest);
			String authHeader = sscUnirest.config().getDefaultHeaders().get("Authorization").stream().filter(h->h.startsWith("FortifyToken")).findFirst().orElseThrow();
			return unirestRunner.runWithUnirest(scDastUnirest->{
				scDastUnirest.config().defaultBaseUrl(scDastApiUrl).setDefaultHeader("Authorization", authHeader);
				return runner.apply(scDastUnirest);
			});
		});
	}

	private String getSCDastApiUrlFromSSC(UnirestInstance sscUnirest) {
		ObjectNode configData = sscUnirest.get("/api/v1/configuration?group=edast").asObject(ObjectNode.class).getBody(); // TODO Check response code
		
		// TODO Can we simplify this without all these intermediate arrays? 
		ArrayNode properties = JsonPath.read(configData, "$.data.properties");
		ArrayNode scDastEnabledArray = JsonPath.read(properties, "$.[?(@.name=='edast.enabled')].value");
		if ( !scDastEnabledArray.hasNonNull(0) || !scDastEnabledArray.get(0).asBoolean(false) ) {
			throw new IllegalStateException("ScanCentral DAST must be enabled in SSC");
		}
		ArrayNode scDastUrlArray = JsonPath.read(properties, "$.[?(@.name=='edast.server.url')].value");
		String scDastUrl = !scDastUrlArray.hasNonNull(0) ? null : scDastUrlArray.get(0).asText();
		if ( StringUtils.isEmpty(scDastUrl) ) {
			throw new IllegalStateException("SSC returns an empty ScanCentral DAST URL");
		}
		// We remove '/api' and any trailing slashes from the URL as most users will specify relative URL's starting with /api/v2/...
		return scDastUrl.replaceAll("/api/?$","").replaceAll("/+$", "");
	}
	
/*
{
  "data": {
    "properties": [
      {
        "name": "edast.enabled",
        "value": "true",
        "group": "edast",
        "subGroup": "",
        "description": "Enable ScanCentral DAST",
        "appliedAfterRestarting": false,
        "version": 4,
        "propertyType": "BOOLEAN",
        "groupSwitchEnabled": true,
        "required": false,
        "protectedOption": false
      },
      {
        "name": "edast.server.url",
        "value": "http://52.175.230.110:5000/api/",
        "group": "edast",
        "subGroup": "",
        "description": "ScanCentral DAST server URL",
        "appliedAfterRestarting": false,
        "version": 14,
        "propertyType": "URL",
        "groupSwitchEnabled": false,
        "required": true,
        "protectedOption": false
      }
    ]
  },
  "responseCode": 200
}	
*/
}
