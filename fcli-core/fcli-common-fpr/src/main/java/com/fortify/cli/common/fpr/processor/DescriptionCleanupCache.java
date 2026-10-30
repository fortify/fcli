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
package com.fortify.cli.common.fpr.processor;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fortify.cli.common.fpr.util.StringUtil;

/**
 * Reuses markup-stripped text within one streaming parse. Rendering remains vulnerability-specific.
 * Both entry count and combined input/output character count are bounded. Not thread-safe.
 */
final class DescriptionCleanupCache {
    private static final int MAX_ENTRIES = 1_024;
    private static final long MAX_CHARACTERS = 1_048_576;

    private final Map<String, String> cleanedDescriptions = new LinkedHashMap<>(16, 0.75f, true);
    private final int maxEntries;
    private final long maxCharacters;
    private long retainedCharacters;

    DescriptionCleanupCache() {
        this(MAX_ENTRIES, MAX_CHARACTERS);
    }

    DescriptionCleanupCache(int maxEntries, long maxCharacters) {
        if (maxEntries <= 0 || maxCharacters <= 0) {
            throw new IllegalArgumentException("Description cache limits must be positive");
        }
        this.maxEntries = maxEntries;
        this.maxCharacters = maxCharacters;
    }

    /** Always strips markup; other cleanup modes must not share this cache. */
    String clean(String description) {
        // Plain text already takes StringUtil's cheap trim path; large inputs must not displace the cache.
        if (description == null || description.length() > maxCharacters || description.indexOf('<') < 0) {
            return StringUtil.stripTags(description, true);
        }
        String cached = cleanedDescriptions.get(description);
        if (cached != null) {
            return cached;
        }

        String cleaned = StringUtil.stripTags(description, true);
        long characters = (long) description.length() + cleaned.length();
        if (characters > maxCharacters) {
            return cleaned;
        }
        while (cleanedDescriptions.size() >= maxEntries || retainedCharacters + characters > maxCharacters) {
            var entries = cleanedDescriptions.entrySet().iterator();
            var oldest = entries.next();
            retainedCharacters -= (long) oldest.getKey().length() + oldest.getValue().length();
            entries.remove();
        }
        cleanedDescriptions.put(description, cleaned);
        retainedCharacters += characters;
        return cleaned;
    }

    void clear() {
        cleanedDescriptions.clear();
        retainedCharacters = 0;
    }
}
