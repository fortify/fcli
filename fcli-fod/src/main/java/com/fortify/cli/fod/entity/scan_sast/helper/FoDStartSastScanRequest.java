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

package com.fortify.cli.fod.entity.scan_sast.helper;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@Reflectable @NoArgsConstructor
@Getter @ToString
public class FoDStartSastScanRequest {
    private String entitlementPreferenceType;
    private Boolean purchaseEntitlement;
    private Integer entitlementId;
    private String remdiationScanPreferenceType;
    private String inProgressScanActionType;
    private String scanMethodType;
    private String scanTool;
    private String scanToolVersion;
    private String notes;


    public FoDStartSastScanRequest setEntitlementPreferenceType(String entitlementPreferenceType) {
        this.entitlementPreferenceType = entitlementPreferenceType;
        return this;
    }

    public FoDStartSastScanRequest setPurchaseEntitlement(Boolean purchaseEntitlement) {
        this.purchaseEntitlement = purchaseEntitlement;
        return this;
    }

    public FoDStartSastScanRequest setEntitlementId(Integer entitlementId) {
        this.entitlementId = entitlementId;
        return this;
    }

    public FoDStartSastScanRequest setRemdiationScanPreferenceType(String remdiationScanPreferenceType) {
        this.remdiationScanPreferenceType = remdiationScanPreferenceType;
        return this;
    }

    public FoDStartSastScanRequest setInProgressScanActionType(String inProgressScanActionType) {
        this.inProgressScanActionType = inProgressScanActionType;
        return this;
    }

    public FoDStartSastScanRequest setScanMethodType(String scanMethodType) {
        this.scanMethodType = scanMethodType;
        return this;

    }

    public FoDStartSastScanRequest setScanTool(String scanTool) {
        this.scanTool = (scanTool == null ? "Other" : scanTool);
        return this;

    }

    public FoDStartSastScanRequest setScanToolVersion(String scanToolVersion) {
        this.scanToolVersion = (scanToolVersion == null ? "N/A" : scanToolVersion);
        return this;
    }

    public FoDStartSastScanRequest setNotes(String notes) {
        this.notes = (notes == null ? "" : notes);
        return this;
    }
}
