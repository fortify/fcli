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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public abstract class AbstractFoDScanSetupCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Log LOG = LogFactory.getLog(AbstractFoDScanSetupCommand.class);


    @Mixin protected FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin protected FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;

    @Option(names = {"--assessment-type"}, required = true)
    protected String assessmentType; // Plain text name as custom assessment types can be created
    @Mixin
    protected FoDEntitlementFrequencyTypeMixins.RequiredOption entitlementFrequencyTypeMixin;
    @Option(names = {"--entitlement-id"})
    protected Integer entitlementId = 0;

    @Mixin
    protected CommonOptionMixins.OptionalFile uploadFileMixin;

    // the File Id previously uploaded or uploaded using "uploadFileToUse" below
    private int fileId = 0;

    protected void setFileId(int fileId) {
        this.fileId = fileId;
    }

    protected int uploadFileToUse(UnirestInstance unirest, String releaseId, FoDScanType scanType, String fileType) {
        int fileIdToUse = 0;
        HttpRequest<?> uploadFileRequest = null;
        switch (scanType) {
            case Dynamic:
                // Only supporting DAST Automated file uploads from fcli for now
                uploadFileRequest = getDastAutomatedUploadFileRequest(unirest, releaseId, fileType);
                break;
            case Static:
            case Mobile:
                // Neither Static or Mobile require any file uploads yet
                break;
            default:
        }
        JsonNode response = FoDFileTransferHelper.upload(unirest, uploadFileRequest, uploadFileMixin.getFile());
        fileIdToUse = response.get("fileId").intValue(); fileId = fileIdToUse;
        return fileIdToUse;
    }

    protected HttpRequest<?> getDastAutomatedUploadFileRequest(UnirestInstance unirest, String releaseId, String dastFileType) {
        return unirest.patch(FoDUrls.DAST_AUTOMATED_SCANS + "/scan-setup/file-upload")
                .routeParam("relId", releaseId)
                .queryString("dastFileType", dastFileType);
    }

    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        var releaseId = releaseDescriptor.getReleaseId();
        HttpRequest<?> request = getBaseRequest(unirest, releaseId);
        HttpResponse<?> response = request.asString(); // successful invocation returns empty response
        return releaseDescriptor.asObjectNode()
                .put("scanType", getScanType())
                .put("setupType", getSetupType())
                .put("filename", (uploadFileMixin.getFile() != null ? uploadFileMixin.getFile().getName() : "N/A"))
                .put("entitlementId", entitlementId)
                .put("fileId", fileId);
    }

    protected abstract String getScanType();
    protected abstract String getSetupType();

    protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId);

    @Override
    public final String getActionCommandResult() {
        return "SETUP";
    }

    @Override
    public final boolean isSingular() {
        return true;
    }
}
