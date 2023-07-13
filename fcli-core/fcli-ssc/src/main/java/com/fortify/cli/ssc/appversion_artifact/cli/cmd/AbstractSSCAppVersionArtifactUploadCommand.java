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
package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;

import kong.unirest.HttpRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

public abstract class AbstractSSCAppVersionArtifactUploadCommand extends AbstractSSCAppVersionArtifactOutputCommand implements IBaseRequestSupplier {
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    
    @Override
    public final HttpRequest<?> getBaseRequest() {
        try ( var progressWriter = progressWriterFactory.create() ) {
            var unirest = getUnirestInstance();
        	String engineType = getEngineType();
            SSCAppVersionDescriptor av = parentResolver.getAppVersionDescriptor(unirest);
            HttpRequestWithBody request = unirest.post(SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getVersionId()));
            if ( StringUtils.isNotBlank(engineType) ) {
            	// TODO Check parser plugin is enabled in SSC
            	request = request.queryString("engineType", engineType);
            }
            File file = getFile();
            preUpload(unirest, progressWriter, file);
            JsonNode uploadResponse = request.multiPartContent()
            		.field("file", file)
            		.asObject(JsonNode.class).getBody();
            postUpload(unirest, progressWriter, file);
            String artifactId = JsonHelper.evaluateSpelExpression(uploadResponse, "data.id", String.class);
            // TODO Do we actually show any scan data from the embedded scans?
            return unirest.get(SSCUrls.ARTIFACT(artifactId)).queryString("embed","scans");
        }
    }

	@Override
    public boolean isSingular() {
        return true;
    }
    
    protected abstract String getEngineType();
    protected abstract File getFile();
    
    protected void preUpload(UnirestInstance unirest, IProgressWriterI18n progressWriter, File file) {}
    protected void postUpload(UnirestInstance unirest, IProgressWriterI18n progressWriter, File file) {}
}
