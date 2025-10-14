/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod.dast_scan.cli.cmd;

import java.util.ArrayList;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanSetupCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedHelper;
import com.fortify.cli.fod.dast_scan.helper.FileUploadResult;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractFoDDastAutomatedScanSetupCommand extends AbstractFoDScanSetupCommand<FoDScanConfigDastAutomatedDescriptor> {
    private static final Log LOG = LogFactory.getLog(AbstractFoDDastAutomatedScanSetupCommand.class);
    @Getter private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mixin
    protected CommonOptionMixins.OptionalFile uploadFileMixin;

    // the File Id previously uploaded or uploaded using "uploadFileToUse" below
    private int fileId = 0;

    protected void setFileId(int fileId) {
        this.fileId = fileId;
    }

    @Override
    protected String getScanType() {
        return "DAST Automated";
    }

    protected FileUploadResult uploadFileToUse(UnirestInstance unirest, String releaseId, FoDScanType scanType, String fileType) {
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
        int fileIdToUse = response.get("fileId").intValue();
        fileId = fileIdToUse;
        ArrayList<String> hosts = response.has("hosts") ?
                objectMapper.convertValue(response.get("hosts"), new com.fasterxml.jackson.core.type.TypeReference<ArrayList<String>>() {}) :
                new ArrayList<>();
        return new FileUploadResult(fileIdToUse, hosts);
    }

    protected HttpRequest<?> getDastAutomatedUploadFileRequest(UnirestInstance unirest, String releaseId, String dastFileType) {
        return unirest.patch(FoDUrls.DAST_AUTOMATED_SCANS + "/scan-setup/file-upload")
                .routeParam("relId", releaseId)
                .queryString("dastFileType", dastFileType);
    }

    @Override
    protected boolean isExistingSetup(FoDScanConfigDastAutomatedDescriptor setupDescriptor) {
        return (setupDescriptor != null && setupDescriptor.getAssessmentTypeId() != 0);
    }

    @Override
    protected ObjectNode convertToObjectNode(FoDScanConfigDastAutomatedDescriptor setupDescriptor) {
        return setupDescriptor.asObjectNode();
    }

    @Override
    protected FoDScanConfigDastAutomatedDescriptor getSetupDescriptor(UnirestInstance unirest, String releaseId) {
        return FoDScanDastAutomatedHelper.getSetupDescriptor(unirest, releaseId);
    }

    protected void validateEntitlement(FoDScanConfigDastAutomatedDescriptor currentSetup, Integer entitlementIdToUse,
                                    String relId, FoDReleaseAssessmentTypeDescriptor atd) {
        // validate entitlement specified or currently in use against assessment type found
        if (entitlementId != null && entitlementId > 0) {
            // check if "entitlement id" explicitly matches what has been found
            if (!Objects.equals(entitlementIdToUse, entitlementId)) {
                throw new FcliSimpleException("Cannot find appropriate assessment type for use with entitlement: " + entitlementId);
            }
        } else {
            if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
                // check if "entitlement id" is already configured
                if (!Objects.equals(entitlementIdToUse, currentSetup.getEntitlementId())) {
                    LOG.warn("Changing current release entitlement from " + currentSetup.getEntitlementId());
                }
            }
        }
        // check if the entitlement can still be used
        if (FoDReleaseAssessmentTypeHelper.validateEntitlementCanBeUsed(relId, atd)) {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is still valid.");
        } else {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is no longer valid.");
        }
    }

}
