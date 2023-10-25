/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.license.ncd_report.generator.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportAuthorDescriptor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * GitLab-specific implementation of {@link INcdReportAuthorDescriptor}.
 * 
 * @author rsenden
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = false)
public class NcdReportGitLabAuthorDescriptor extends JsonNodeHolder implements INcdReportAuthorDescriptor {
    @JsonProperty("author_name")
    private String name;
    @JsonProperty("author_email")
    private String email;
}
