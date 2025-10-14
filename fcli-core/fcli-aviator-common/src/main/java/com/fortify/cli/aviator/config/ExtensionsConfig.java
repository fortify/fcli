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

import java.util.HashMap;
import java.util.Map;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.aviator.util.StringUtil;

import lombok.NoArgsConstructor;

@NoArgsConstructor @Reflectable
public class ExtensionsConfig {
    private Map<String, String> supportedExtensions = new HashMap<>();

    public void setSupportedExtensions(Map<String, String> extensions) {
        this.supportedExtensions = extensions != null ? new HashMap<>(extensions) : new HashMap<>();
    }

    public Map<String, String> getExtensions() {
        return new HashMap<>(supportedExtensions);
    }

    public String getLanguageForExtension(String extension) {
        if (StringUtil.isEmpty(extension)) {
            return "Unknown";
        }
        var ext = extension.startsWith(".")
                ? extension
                : "." + extension;

        return supportedExtensions.getOrDefault(ext, "Unknown");
    }
}
