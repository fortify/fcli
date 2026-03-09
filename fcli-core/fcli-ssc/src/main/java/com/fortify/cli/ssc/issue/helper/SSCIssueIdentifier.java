/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.ssc.issue.helper;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;

import lombok.Data;
import lombok.experimental.Accessors;

@JsonInclude(Include.NON_NULL)
@Reflectable
@Data @Accessors(fluent=true)
public final class SSCIssueIdentifier {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("revision")
    private Integer revision;
    
    public static final SSCIssueIdentifier fromIdAndRevision(String id, Integer revision) {
        return new SSCIssueIdentifier().id(id).revision(revision);
    }
    
    public static final List<SSCIssueIdentifier> fromIdList(List<String> ids) {
        return ids.stream()
                .map(id -> SSCIssueIdentifier.fromIdAndRevision(id, null))
                .toList();
    }
    
    @Override
    public String toString() {
        try {
            return JsonHelper.getObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            return super.toString();
        }
    }
}