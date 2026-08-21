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
package com.fortify.cli.aviator.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.fpr.Vulnerability;

class CorrelationStreamProcessorTest {

    @Test
    void buildCorrelationWorkItemsPreservesDastSessionUrlEncounterOrder() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, null, null, null, 0, 0);
        CorrelationStreamProcessor.CorrelationBucketData bucket = createBucket(
            "SQL Injection",
            List.of("https://example.com/z", "https://example.com/login", "https://example.com/z", "https://example.com/a")
        );

        List<?> items = invokeBuildCorrelationWorkItems(processor, List.of(bucket));

        assertEquals(1, items.size());
        assertIterableEquals(
            List.of("https://example.com/z", "https://example.com/login", "https://example.com/a"),
            getDastUrls(items.get(0))
        );
    }

    @Test
    void buildUrlToDastMapPreservesUrlEncounterOrder() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, null, null, null, 0, 0);
        CorrelationStreamProcessor.CorrelationBucketData bucket = createBucket(
            "SQL Injection",
            List.of("https://example.com/z", "https://example.com/login", "https://example.com/z", "https://example.com/a")
        );

        Map<String, List<DastIssue>> urlMap = invokeBuildUrlToDastMap(processor, List.of(bucket));

        assertIterableEquals(
            List.of("https://example.com/z", "https://example.com/login", "https://example.com/a"),
            urlMap.keySet()
        );
    }

    private CorrelationStreamProcessor.CorrelationBucketData createBucket(String category, List<String> sessionUrls) {
        List<DastIssue> dastIssues = sessionUrls.stream()
            .map(this::createDastIssue)
            .toList();
        Vulnerability vulnerability = Vulnerability.builder().instanceID("SAST-1").build();
        return new CorrelationStreamProcessor.CorrelationBucketData(category, List.of(vulnerability), dastIssues);
    }

    private DastIssue createDastIssue(String sessionUrl) {
        DastIssue issue = new DastIssue();
        issue.setId(sessionUrl);
        issue.setSessionUrl(sessionUrl);
        return issue;
    }

    @SuppressWarnings("unchecked")
    private List<?> invokeBuildCorrelationWorkItems(CorrelationStreamProcessor processor, List<?> mixedBuckets)
            throws Exception {
        Method method = CorrelationStreamProcessor.class.getDeclaredMethod("buildCorrelationWorkItems", List.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(processor, mixedBuckets);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<DastIssue>> invokeBuildUrlToDastMap(CorrelationStreamProcessor processor, List<?> mixedBuckets)
            throws Exception {
        Method method = CorrelationStreamProcessor.class.getDeclaredMethod("buildUrlToDastMap", List.class);
        method.setAccessible(true);
        return (Map<String, List<DastIssue>>) method.invoke(processor, mixedBuckets);
    }

    @SuppressWarnings("unchecked")
    private List<String> getDastUrls(Object workItem) throws Exception {
        Method method = workItem.getClass().getDeclaredMethod("dastUrls");
        method.setAccessible(true);
        return (List<String>) method.invoke(workItem);
    }
}