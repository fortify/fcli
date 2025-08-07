package com.fortify.cli.aviator.fpr.filter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AttributeMapper {

    private static final Map<String, String> MODIFIER_TO_ATTRIBUTE_MAP;

    static {
        MODIFIER_TO_ATTRIBUTE_MAP = Stream.of(new String[][]{
                {"accuracy", "accuracy"},
                {"analysis", "issuestate"},
                {"analysis type", "analysistype"},
                {"[analysis type]", "analysistype"},
                {"analyzer", "analyzer"},
                {"attack payload", "attackpayload"},
                {"attack type", "attacktype"},
                {"audience", "audience"},
                {"audited", "audited"},
                {"body", "requestbody"},
                {"bug", "bugid"},
                {"cat", "category"},
                {"category", "category"},
                {"[category]", "category"},
                {"class", "classname"},
                {"codesnippet", "codesnippet"},
                {"comment", "comment"},
                {"comments", "comment"},
                {"commentuser", "commentuser"},
                {"con", "confidence"},
                {"confidence", "confidence"},
                {"cookies", "requestcookies"},
                {"correlated", "correlated"},
                {"cwe", "cwe"},
                {"engine priority", "enginepriority"},
                {"file", "filename"},
                {"filetype", "filetype"},
                {"fortify priority order", "priority"},
                {"[fortify priority order]", "priority"},
                {"headers", "requestheaders"},
                {"historyuser", "historyuser"},
                {"http version", "requesthttpversion"},
                {"impact", "impact"},
                {"instance id", "instanceid"},
                {"issue age", "issueage"},
                {"issue state", "issuestate"},
                {"[issue state]", "issuestate"},
                {"kingdom", "kingdom"},
                {"likelihood", "likelihood"},
                {"line", "linenumber"},
                {"manual", "manual"},
                {"mapped category", "mappedcategory"},
                {"method", "requestmethod"},
                {"package", "package"},
                {"parameters", "requestparameters"},
                {"primaryrule", "classid"},
                {"probability", "probability"},
                {"remediation effort", "remediation"},
                {"request id", "requestid"},
                {"response", "response"},
                {"rule", "classid"},
                {"ruleid", "classid"},
                {"secondary requests", "secondaryrequests"},
                {"sev", "instanceseverity"},
                {"severity", "instanceseverity"},
                {"shortfilename", "shortfilename"},
                {"sink", "sinkfunction"},
                {"[sink function]", "sinkfunction"},
                {"source", "sourcefunction"},
                {"[source function]", "sourcefunction"},
                {"source context", "sourcecontext"},
                {"sourcefile", "sourcefile"},
                {"sourceline", "sourceline"},
                {"status", "issuestatus"},
                {"[issue status]", "issuestatus"},
                {"suppressed", "suppressed"},
                {"taint", "taintflags"},
                {"[taint flags]", "taintflags"},
                {"trace", "tracenode"},
                {"tracenode", "tracenode"},
                {"tracenodeallpaths", "tracenode"},
                {"trigger", "trigger"},
                {"url", "url"},
                {"user", "user"},

                // --- Custom Tag Style Attributes ---
                {"pci 4.0", "pci 4.0"},
                {"[pci 4.0]", "pci 4.0"},
                {"aa_prediction", "aa_prediction"},
                {"[aa_prediction]", "aa_prediction"}

        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    }

    public static String getAttributeName(String modifier) {
        if (modifier == null) {
            return null;
        }
        String attributeName = MODIFIER_TO_ATTRIBUTE_MAP.get(modifier);

        if (attributeName == null) {
            return modifier;
        }

        return attributeName;
    }
}