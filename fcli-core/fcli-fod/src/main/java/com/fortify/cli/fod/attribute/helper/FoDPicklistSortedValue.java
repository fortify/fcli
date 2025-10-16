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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FoDPicklistSortedValue extends FoDPicklistValue {
    private int SortOrder;

    public FoDPicklistSortedValue(String name, int sortOrder) {
        super(name);
        this.SortOrder = sortOrder;
    }
}
