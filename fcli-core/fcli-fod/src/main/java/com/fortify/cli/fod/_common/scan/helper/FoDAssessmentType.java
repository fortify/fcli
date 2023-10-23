/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.fod._common.scan.helper;

public enum FoDAssessmentType {
    Static(1, 4),
    StaticPlus(2, 6),
    DynamicWebsite(2, 6),
    DynamicPlusWebsite(6, 18),
    DynamicAPI(2, 6),
    DynamicPlusAPI(6, 18),
    Mobile(1, 4),
    MobilePlus(6, 18);

    public final int singleUnits;
    public final int subscriptionUnits;

    FoDAssessmentType(int singleUnits, int subscriptionUnits) {
        this.singleUnits = singleUnits;
        this.subscriptionUnits = subscriptionUnits;
    }

    public int getSingleUnits() {
        return this.singleUnits;
    }

    public int getSubscriptionUnits() {
        return this.subscriptionUnits;
    }

    public FoDScanType toScanType() {
        switch (this) {
            case DynamicWebsite:
            case DynamicPlusWebsite:
            case DynamicAPI:
            case DynamicPlusAPI:
                    return FoDScanType.Dynamic;
            case Mobile:
            case MobilePlus:
                    return FoDScanType.Mobile;
            default:
                return FoDScanType.Static;
        }
    }

}