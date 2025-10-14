package com.fortify.cli.aviator.fpr.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class FilterSet {
    private String type;
    private String id;
    private boolean enabled;
    private boolean disableEdit;
    private String title;
    private String description;
    private List<String> enabledFolders;
    private List<Filter> filters;
    private List<FolderDefinition> folderDefinitions;
}