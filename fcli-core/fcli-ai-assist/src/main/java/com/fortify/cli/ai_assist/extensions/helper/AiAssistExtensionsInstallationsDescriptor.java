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
package com.fortify.cli.ai_assist.extensions.helper;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight fcli state descriptor stored at
 * {@code state/ai-assist/extensions/installations.json}.
 * Records which assistants extensions were set up for and their resolved
 * target directories. Used by {@code list-installed} and {@code uninstall}
 * to work offline without needing the distribution descriptor.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder = true) @Data
public class AiAssistExtensionsInstallationsDescriptor {

    /**
     * Map from assistant ID to its installation entry.
     */
    private Map<String, AssistantInstallation> assistants;

    public Map<String, AssistantInstallation> getAssistants() {
        if (assistants == null) { assistants = new LinkedHashMap<>(); }
        return assistants;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    @Reflectable @NoArgsConstructor @AllArgsConstructor @Builder @Data
    public static class AssistantInstallation {
        @JsonProperty("display-name")
        private String displayName;
        /**
         * Map from content type (e.g., "skills", "agents") to resolved target directory path.
         */
        private Map<String, String> targets;

        public Map<String, String> getTargets() {
            if (targets == null) { targets = new LinkedHashMap<>(); }
            return targets;
        }
    }
}
