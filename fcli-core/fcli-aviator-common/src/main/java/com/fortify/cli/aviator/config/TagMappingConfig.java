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