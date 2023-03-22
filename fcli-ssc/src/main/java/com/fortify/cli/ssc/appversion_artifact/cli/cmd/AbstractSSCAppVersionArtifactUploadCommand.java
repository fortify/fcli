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
package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.HttpRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

public abstract class AbstractSSCAppVersionArtifactUploadCommand extends AbstractSSCAppVersionArtifactOutputCommand implements IUnirestBaseRequestSupplier {
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    
    @Override
    public final HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
    	String engineType = getEngineType();
        SSCAppVersionDescriptor av = parentResolver.getAppVersionDescriptor(unirest);
        HttpRequestWithBody request = unirest.post(SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getVersionId()));
        if ( StringUtils.isNotBlank(engineType) ) {
        	// TODO Check parser plugin is enabled in SSC
        	request = request.queryString("engineType", engineType);
        }
        File file = getFile();
        preUpload(unirest, file);
        JsonNode uploadResponse = request.multiPartContent()
        		.field("file", file)
        		.asObject(JsonNode.class).getBody();
        postUpload(unirest, file);
        String artifactId = JsonHelper.evaluateSpELExpression(uploadResponse, "data.id", String.class);
        // TODO Do we actually show any scan data from the embedded scans?
        return unirest.get(SSCUrls.ARTIFACT(artifactId)).queryString("embed","scans");
    }

	@Override
    public boolean isSingular() {
        return true;
    }
    
    protected abstract String getEngineType();
    protected abstract File getFile();
    
    protected void preUpload(UnirestInstance unirest, File file) {}
    protected void postUpload(UnirestInstance unirest, File file) {}
}
