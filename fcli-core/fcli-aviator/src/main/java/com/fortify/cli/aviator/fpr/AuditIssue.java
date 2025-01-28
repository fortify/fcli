package com.fortify.cli.aviator.fpr;

import com.formkiq.graalvm.annotations.Reflectable;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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