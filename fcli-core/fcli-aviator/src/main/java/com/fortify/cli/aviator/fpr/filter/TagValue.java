package com.fortify.cli.aviator.fpr.filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TagValue {
    private boolean isDefault;
    private String id;
    private String description;
    private boolean hidden;
    private String value;
}