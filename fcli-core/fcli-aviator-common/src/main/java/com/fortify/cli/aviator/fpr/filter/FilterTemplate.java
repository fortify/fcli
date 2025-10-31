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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterTemplate {
    private String version;
    private boolean disableEdit;
    private String id;
    private int objectVersion;
    private int publishVersion;
    private String name;
    private String description;
    private List<FolderDefinition> folderDefinitions;
    private String defaultFolder;
    private List<TagDefinition> tagDefinitions;
    private PrimaryTag primaryTag;
    private List<FilterSet> filterSets;
}
