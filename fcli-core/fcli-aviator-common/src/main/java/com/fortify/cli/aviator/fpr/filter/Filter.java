package com.fortify.cli.aviator.fpr.filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Filter {
    private String actionParam;
    private String query;
    private String action;
}