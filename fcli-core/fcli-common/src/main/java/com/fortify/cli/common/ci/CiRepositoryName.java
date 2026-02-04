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
package com.fortify.cli.common.ci;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;

/**
 * Repository name information, providing both short and full qualified names.
 * Used by both local Git repositories and CI systems to provide consistent naming.
 * 
 * <p>Examples:
 * <ul>
 *   <li>GitHub: short="fcli", full="fortify/fcli"</li>
 *   <li>GitLab: short="fcli", full="fortify/team/fcli"</li>
 *   <li>Local: short="fcli", full="fortify/fcli" (from remote URL)</li>
 * </ul>
 * 
 * @param short_ Short repository name (just the repository name without path/owner)
 * @param full Full qualified name (owner/repo or group/subgroup/repo)
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record CiRepositoryName(
    @JsonProperty("short") String short_,
    String full
) {}
