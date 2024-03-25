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
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@CommandGroup("*-scan-upload-file")
public abstract class AbstractFoDScanFileUploadCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;

    @Mixin private CommonOptionMixins.RequiredFile uploadFileMixin;

    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        var releaseId = releaseDescriptor.getReleaseId();
        HttpRequest<?> baseRequest = getBaseRequest(unirest, releaseId);
        JsonNode response = FoDFileTransferHelper.upload(unirest, baseRequest, uploadFileMixin.getFile());
        return releaseDescriptor.asObjectNode()
                .put("fileType", getFileType())
                .put("filename", uploadFileMixin.getFile().getName())
                .put("fileId", response.get("fileId").intValue());
    }

    protected abstract String getFileType();

    protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId);

    @Override
    public final String getActionCommandResult() {
        return "UPLOADED";
    }

    @Override
    public final boolean isSingular() {
        return true;
    }

}
