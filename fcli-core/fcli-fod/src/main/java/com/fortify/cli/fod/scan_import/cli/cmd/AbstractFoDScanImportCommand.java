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

package com.fortify.cli.fod.scan_import.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.core.HttpRequest;
import kong.unirest.core.UnirestInstance;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

public abstract class AbstractFoDScanImportCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;

    @Parameters(index = "0", arity = "1")
    private File scanFile;
    
    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        var releaseId = releaseDescriptor.getReleaseId();
        var importScanSessionId = getImportScanSessionId(unirest, releaseId);
        HttpRequest<?> baseRequest = getBaseRequest(unirest, releaseId)
                .queryString("importScanSessionId", importScanSessionId)
                .queryString("fileLength", scanFile.length());
        FoDFileTransferHelper.uploadChunked(unirest, baseRequest, scanFile);
        return releaseDescriptor.asJsonNode();
    }
    
    protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId);

    @Override
    public final String getActionCommandResult() {
        return "IMPORT_REQUESTED";
    }

    @Override
    public final boolean isSingular() {
        return true;
    }
    
    private static final String getImportScanSessionId(UnirestInstance unirest, String relId) {
        return unirest.get(FoDUrls.RELEASE_IMPORT_SCAN_SESSION)
                .routeParam("relId", relId)
                .asObject(ObjectNode.class)
                .getBody()
                .get("importScanSessionId")
                .asText();
    }
}
