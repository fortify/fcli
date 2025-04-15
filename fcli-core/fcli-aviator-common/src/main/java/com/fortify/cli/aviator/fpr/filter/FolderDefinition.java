package com.fortify.cli.aviator.fpr.filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class FolderDefinition {
    private String id;
    private String color;
    private String name;
    private String description;
}