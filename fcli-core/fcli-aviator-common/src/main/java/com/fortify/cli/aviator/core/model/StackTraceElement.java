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
public class StackTraceElement {
    private String filename;
    private int line;
    private final String code;
    private String nodeType;
    private final List<StackTraceElement> innerStackTrace;
    private final Fragment fragment;
    private String additionalInfo;
    private String taintflags;

    public StackTraceElement(String filename, int line, String code, String nodeType, List<StackTraceElement> innerStackTrace, Fragment fragment, String additionalInfo, String taintflags) {
        this.filename = filename;
        this.line = line;
        this.code = code;
        this.nodeType = nodeType == null ? "" : nodeType;
        this.innerStackTrace = innerStackTrace != null ? innerStackTrace : new ArrayList<>();
        this.fragment = fragment;
        this.additionalInfo = additionalInfo;
        this.taintflags = taintflags;
    }

    public void setInnerStackTrace(List<StackTraceElement> innerStackTrace) {
        this.innerStackTrace.clear();
        this.innerStackTrace.addAll(innerStackTrace);
    }
}