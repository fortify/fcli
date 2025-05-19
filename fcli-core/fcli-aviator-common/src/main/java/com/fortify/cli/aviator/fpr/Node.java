package com.fortify.cli.aviator.fpr;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Reflectable
public class Node {
    private String id;
    private String filePath;
    private int line;
    private int lineEnd;
    private int colStart;
    private int colEnd;
    private String contextId;
    private String snippet;
    private String actionType;
    private String additionalInfo;
    private String associatedRuleId;
}