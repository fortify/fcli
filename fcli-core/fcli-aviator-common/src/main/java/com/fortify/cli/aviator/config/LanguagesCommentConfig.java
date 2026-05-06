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
package com.fortify.cli.aviator.config;

import java.util.HashMap;
import java.util.Map;

import com.fortify.cli.aviator.util.StringUtil;

public class LanguagesCommentConfig {
    private Map<String, String> lineCommentSymbols = new HashMap<>();

    public void setLineCommentSymbols(Map<String, String> comments) {
        this.lineCommentSymbols = comments != null ? new HashMap<>(comments) : new HashMap<>();
    }

    public Map<String, String> getLineCommentSymbols() {
        return new HashMap<>(lineCommentSymbols);
    }

    public String getCommentForLanguage(String language) {
        if (StringUtil.isEmpty(language)) {
            return "Unknown";
        }

        return lineCommentSymbols.getOrDefault(language, "Unknown");
    }
}
