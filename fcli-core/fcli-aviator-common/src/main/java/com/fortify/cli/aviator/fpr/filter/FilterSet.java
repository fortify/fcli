package com.fortify.cli.aviator.fpr.filter;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterSet {
    private String type;
    private String id;
    private boolean enabled;
    private boolean disableEdit;
    private String title;
    private String description;
    private List<String> enabledFolders;
    private List<Filter> filters;
}