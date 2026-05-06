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
package com.fortify.cli.aviator.util;

import com.fortify.cli.aviator.config.LanguagesCommentConfig;

public class LanguageCommentMapperUtil {
    private static LanguagesCommentConfig commentsConfig;

    public static void initializeConfig(LanguagesCommentConfig loadedCommentsConfig) {
        commentsConfig = loadedCommentsConfig;
    }

    public static String getProgrammingLanguageComment(String language) {
        if(StringUtil.isEmpty(language)){
            return "Unknown";
        }

        return commentsConfig != null
                ? commentsConfig.getCommentForLanguage(language)
                : "Unknown";
    }
}
