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
package com.fortify.cli.aviator.config;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;

@Data @Reflectable
public class TagMappingConfig {
    private String tag_id = "87f2364f-dcd4-49e6-861d-f8d3f351686b";
    private Mapping mapping;

    @Data
    public static class Mapping {
        private Tier tier_1;
        private Tier tier_2;
    }

    @Data
    public static class Tier {
        private Result fp;
        private Result tp;
        private Result unsure;
    }

    @Data
    public static class Result {
        private String value;
        private Boolean suppress = false;
    }
}