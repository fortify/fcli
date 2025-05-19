package com.fortify.cli.aviator.core.model;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@Reflectable
public class AnalysisInfo {
    private String shortDescription;
    private String explanation;
}
