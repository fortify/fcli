/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.aviator.fpr.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Reflectable
public class AuditIssue {
    private String instanceId;
    private boolean suppressed;
    private int revision;
    @Builder.Default private Map<String, String> tags = new HashMap<>();
    @Builder.Default private List<Comment> threadedComments = new ArrayList<>();

    public void addTag(String tagId, String tagValue) {
        if (tagId != null) {
            tags.put(tagId, tagValue);
        }
    }

    public void addComment(Comment comment) {
        if (comment != null) {
            threadedComments.add(comment);
        }
    }

    @Getter
    @Builder
    @Reflectable
    public static class Comment {
        private String content;
        private String username;
        private String timestamp;
    }
}