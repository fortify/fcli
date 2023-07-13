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
package com.fortify.cli.util.ncd_report.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util.ncd_report.config.NcdReportConfig;
import com.fortify.cli.util.ncd_report.config.NcdReportContributorConfig;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportAuthorDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor.NcdReportProcessedAuthorState;
import com.fortify.cli.util.ncd_report.writer.INcdReportAuthorsWriter;
import com.fortify.cli.util.ncd_report.writer.NcdReportResultsWriters;

import lombok.SneakyThrows;

/**
 * <p>This class is responsible for collecting {@link INcdReportAuthorDescriptor}
 * instances, and updating the report based on the collected instances. For each
 * author passed to the {@link #reportAuthor(INcdReportAuthorDescriptor)} method,
 * this class will decide whether the author should be ignored or considered a
 * contributing author.</p>
 * 
 * <p>If the author is being ignored, it will be immediately written to the output CSV 
 * file, such that all ignored authors are listed first. Contributing authors are 
 * collected using {@link NcdReportAuthorDeduplicator} to identify duplicate authors, 
 * and written to the output CSV file once all authors have been collected.</p>
 * 
 * <p>During these operations, the inner {@link AuthorCounters} class is used to
 * keep track of author counts based on the states defined in the {@link AuthorCounter}
 * enumeration. Author counts are then included in the report summary once all
 * authors have been processed.</p>
 * 
 * @author rsenden
 *
 */
final class NcdReportAuthorCollector {
    private final NcdReportResultsWriters writers;
    private final ObjectNode summary;
    
    private final Map<INcdReportAuthorDescriptor, NcdReportProcessedAuthorDescriptor> processedAuthors = new HashMap<>();
    private final NcdReportAuthorDeduplicator deduplicator;
    private final Optional<NcdReportContributorConfig> contributorConfig;
    private final AuthorCounters counters = new AuthorCounters();
    
    public NcdReportAuthorCollector(NcdReportConfig reportConfig, NcdReportResultsWriters writers, ObjectNode summary) {
        this.writers = writers;
        this.summary = summary;
        this.contributorConfig = reportConfig.getContributor();
        this.deduplicator = new NcdReportAuthorDeduplicator(contributorConfig);
    }

    NcdReportProcessedAuthorDescriptor reportAuthor(INcdReportAuthorDescriptor descriptor) {
        return processedAuthors.computeIfAbsent(descriptor, this::processAuthorDescriptor);
    }
    
    private NcdReportProcessedAuthorDescriptor processAuthorDescriptor(INcdReportAuthorDescriptor authorDescriptor) {
        counters.increaseCount(AuthorCounter.total);
        var expressionInput = authorDescriptor.toExpressionInput();
        if ( isIgnored(expressionInput) ) {
            var result = new NcdReportProcessedAuthorDescriptor(authorDescriptor, NcdReportProcessedAuthorState.ignored, -1, expressionInput);
            writers.authorsWriter().writeIgnoredAuthor(result);
            counters.increaseCount(AuthorCounter.ignored);
            return result;
        } else {
            var result = new NcdReportProcessedAuthorDescriptor(authorDescriptor, NcdReportProcessedAuthorState.processed, counters.increaseCount(AuthorCounter.nonIgnored), expressionInput);
            deduplicator.addAuthor(result);
            return result;
        }
    }
    
    private boolean isIgnored(ObjectNode expressionInput) {
        return contributorConfig
                .flatMap(NcdReportContributorConfig::getIgnoreExpression)
                .map(expr->JsonHelper.evaluateSpelExpression(expressionInput, expr, Boolean.class))
                .orElse(false);
    }
    
    @SneakyThrows
    final void writeResults() {
        deduplicator.getDeduplicatedAuthors().entrySet().forEach(this::writeEntry);
        summary.set("authorCount", counters.toJson());
    }
    
    private final void writeEntry(Entry<NcdReportProcessedAuthorDescriptor, Set<NcdReportProcessedAuthorDescriptor>> e) {
        var contributingAuthorNumber = counters.increaseCount(AuthorCounter.contributing);
        INcdReportAuthorsWriter writer = writers.authorsWriter();
        writer.writeContributor(e.getKey(), contributingAuthorNumber);
        e.getValue().forEach(d->{
            writer.writeDuplicateAuthor(d, contributingAuthorNumber);
            counters.increaseCount(AuthorCounter.duplicate);
        });
    }
    
    private static enum AuthorCounter {
        total, contributing, ignored, nonIgnored, duplicate
    }
    
    private static final class AuthorCounters {
        private final Map<AuthorCounter, Integer> counts = new HashMap<>();
        
        int increaseCount(AuthorCounter counter) {
            int next = getNextIndex(counter);
            counts.put(counter, next);
            return next;
        }
        
        private int getNextIndex(AuthorCounter counter) {
            return counts.getOrDefault(counter, 0)+1;
        }
        
        ObjectNode toJson() {
            var node = JsonHelper.getObjectMapper().createObjectNode();
            Stream.of(AuthorCounter.values())
                .forEach(v->node.put(v.name(), counts.getOrDefault(v, 0)));
            return node;
        }
    }
}
