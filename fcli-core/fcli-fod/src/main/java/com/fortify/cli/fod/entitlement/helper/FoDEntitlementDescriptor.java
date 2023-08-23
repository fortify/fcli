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

package com.fortify.cli.fod.entitlement.helper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Reflectable
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class FoDEntitlementDescriptor extends JsonNodeHolder {
    Integer entitlementId;
    String entitlementDescription;
    Integer unitsPurchased;
    Integer unitsConsumed;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyy-MM-dd'T'hh:mm:ss")
    Date startDate;
    //  e.g. "startDate": "2023-02-23T00:00:00",
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyy-MM-dd'T'hh:mm:ss")
    Date endDate;
//  e.g. "endDate": "2024-02-23T23:59:59",
}
