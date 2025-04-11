package com.fortify.cli.aviator.core.model;

import com.formkiq.graalvm.annotations.Reflectable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode
@Builder
@Reflectable
@Setter
public class StackTraceElement {
    private String filename;
    private int line;
    private final String code;
    private String nodeType;
    private final List<StackTraceElement> innerStackTrace = new ArrayList<>();
    private final Fragment fragment;
    private String additionalInfo;
    private String taintflags;

    public StackTraceElement(String filename, int line, String code, String nodeType, Fragment fragment, String additionalInfo, String taintflags){
        this.filename = filename;
        this.line = line;
        this.code = code;
        this.nodeType = nodeType == null ? "" : nodeType;
        this.fragment = fragment;
        this.additionalInfo = additionalInfo;
        this.taintflags = taintflags;
    }

    public void setInnerStackTrace(List<StackTraceElement> innerStackTrace){
        this.innerStackTrace.clear();
        this.innerStackTrace.addAll(innerStackTrace);
    }
}