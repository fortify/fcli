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
package com.fortify.cli.fod._common.scan.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDescriptor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public class FoDScanDescriptor extends JsonNodeHolder {
    private String scanId;
    private String scanType;
    private String applicationName;
    private String releaseName;
    private String applicationId;
    private String releaseId;
    private String microserviceName;
    private String analysisStatusType;
    private String status;
    private ArrayList<FoDAttributeDescriptor> attributes;

    @JsonIgnore
    public String getReleaseAndScanId() {
        return String.format("%s:%s", releaseId, scanId);
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyy-MM-dd'T'hh:mm:ss")
    private Date startedDateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyy-MM-dd'T'hh:mm:ss")
    private Date completedDateTime;

    public Map<Integer, String> attributesAsMap() {
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, String> attrMap = new HashMap<>();
        for (FoDAttributeDescriptor attr : attributes) {
            attrMap.put(attr.getId(), attr.getValue());
        }
        return Collections.unmodifiableMap(attrMap);
    }
}
