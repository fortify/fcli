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
package com.fortify.cli.fod.attribute.helper;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.*;

@Reflectable @NoArgsConstructor @AllArgsConstructor
@Getter @ToString @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FoDAttributeCreateRequest {
    private String name;
    private String attributeType;
    private String attributeDataType;
    @Builder.Default
    private Boolean isRequired = false;
    @Builder.Default
    private Boolean isRestricted = false;
    private List<FoDPicklistSortedValue> picklistValues;
}
