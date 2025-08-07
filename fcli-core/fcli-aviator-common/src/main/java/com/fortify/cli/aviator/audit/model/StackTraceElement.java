package com.fortify.cli.aviator.audit.model;


import com.fortify.cli.aviator.fpr.utils.Searchable;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.formkiq.graalvm.annotations.Reflectable;
import lombok.EqualsAndHashCode;

@Getter
@EqualsAndHashCode
@Reflectable
@Setter
public class StackTraceElement implements Searchable {
    private String filename;
    private int line;
    private final String code;
    private String nodeType;
    private final List<StackTraceElement> innerStackTrace = new ArrayList<>();
    private final Fragment fragment;
    private String additionalInfo;
    private String taintflags;
    private boolean isDefault = false; // From Node isDefault attribute
    private String reason = ""; // From Reason (text or concatenated RuleID/Internal)
    private Map<String, String> knowledge = new HashMap<>(); // From Node Knowledge/Fact

    public StackTraceElement(String filename, int line, String code, String nodeType, Fragment fragment, String additionalInfo, String taintflags) {
        this.filename = filename;
        this.line = line;
        this.code = code;
        this.nodeType = nodeType == null ? "" : nodeType;
        this.fragment = fragment;
        this.additionalInfo = additionalInfo;
        this.taintflags = taintflags;
    }

    public void setInnerStackTrace(List<StackTraceElement> innerStackTrace) {
        this.innerStackTrace.clear();
        this.innerStackTrace.addAll(innerStackTrace);
    }

    @Override
    public boolean contains(String searchString) {
        if (searchString == null || searchString.isEmpty()) {
            return false;
        }
        String lowerSearch = searchString.toLowerCase();
        return (filename != null && filename.toLowerCase().contains(lowerSearch))
                || (nodeType != null && nodeType.toLowerCase().contains(lowerSearch))
                || (additionalInfo != null && additionalInfo.toLowerCase().contains(lowerSearch))
                || (taintflags != null && taintflags.toLowerCase().contains(lowerSearch))
                || (reason != null && reason.toLowerCase().contains(lowerSearch))
                || (code != null && code.toLowerCase().contains(lowerSearch))
                || knowledge.values().stream().anyMatch(v -> v != null && v.toLowerCase().contains(lowerSearch));
    }

    @Override
    public boolean matches(String matchString) {
        if (matchString == null || matchString.isEmpty()) {
            return false;
        }
        String lowerMatch = matchString.toLowerCase();
        return (filename != null && filename.toLowerCase().equals(lowerMatch))
                || (nodeType != null && nodeType.toLowerCase().equals(lowerMatch))
                || (additionalInfo != null && additionalInfo.toLowerCase().equals(lowerMatch))
                || (taintflags != null && taintflags.toLowerCase().equals(lowerMatch))
                || (reason != null && reason.toLowerCase().equals(lowerMatch))
                || knowledge.values().stream().anyMatch(v -> v != null && v.toLowerCase().equals(lowerMatch));
    }

    @Override
    public boolean matchesPattern(Pattern pattern) {
        if (pattern == null) {
            return false;
        }
        return (filename != null && pattern.matcher(filename).matches())
                || (nodeType != null && pattern.matcher(nodeType).matches())
                || (additionalInfo != null && pattern.matcher(additionalInfo).matches())
                || (taintflags != null && pattern.matcher(taintflags).matches())
                || (reason != null && pattern.matcher(reason).matches())
                || (code != null && pattern.matcher(code).matches())
                || knowledge.values().stream().anyMatch(v -> v != null && pattern.matcher(v).matches());
    }
}