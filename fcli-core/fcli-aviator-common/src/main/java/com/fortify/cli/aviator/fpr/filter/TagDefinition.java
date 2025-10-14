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
package com.fortify.cli.aviator.fpr.filter;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TagDefinition {
    private String id;
    private String type;
    private boolean extensible;
    private boolean hidden;
    private boolean isIconTag;
    private String restriction;
    private int objectVersion;
    private String valueType;
    private String name;
    private String description;
    private List<TagValue> values;
    private List<String> valuesStr;

    public TagDefinition(String name, String id, List<String> values, boolean extensible){
        this.name = name;
        this.id = id;
        this.valuesStr = values;
        this.extensible = extensible;
    }

    public List<String> getTagValuesAsString() {
        return values.stream()
                .map(TagValue::getValue)
                .collect(Collectors.toList());
    }
}