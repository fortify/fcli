/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.fod._common.session.helper;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Enum representing the delivery types for Multi-Factor Authentication (MFA) codes in Fortify on Demand (FoD).
 * @author Sangamesh Vijaykumar
 */
@Reflectable
public enum FoDMfaDeliveryType {
    Email("EmailDelivery"),
    SMS("SMSDelivery");

    private final String apiValue;

    FoDMfaDeliveryType(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }
}
