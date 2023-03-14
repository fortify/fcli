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

package com.fortify.cli.fod.sast_scan.helper;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@ReflectiveAccess
@Getter
@ToString
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
