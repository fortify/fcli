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

package com.fortify.cli.fod._common.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@CommandGroup("*-scan-import")
public abstract class AbstractFoDScanImportCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;

    @Mixin private CommonOptionMixins.RequiredFile scanFileMixin;
    
    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        var releaseId = releaseDescriptor.getReleaseId();
        var importScanSessionId = getImportScanSessionId(unirest, releaseId);
        HttpRequest<?> baseRequest = getBaseRequest(unirest, releaseId)
                .queryString("importScanSessionId", importScanSessionId)
                .queryString("fileLength", scanFileMixin.getFile().length());
        FoDFileTransferHelper.uploadChunked(unirest, baseRequest, scanFileMixin.getFile());
        return releaseDescriptor.asObjectNode()
                .put("importScanSessionId", importScanSessionId)
                .put("scanType", getScanType().name());
    }

    protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId);

    protected abstract FoDScanType getScanType();

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
