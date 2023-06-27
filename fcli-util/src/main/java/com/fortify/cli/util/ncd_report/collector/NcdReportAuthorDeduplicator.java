/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.ncd_report.collector;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util.ncd_report.config.NcdReportContributorConfig;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

import lombok.Getter;

/**
 * <p>This class is responsible for de-duplicating authors passed to the 
 * {@link #addAuthor(NcdReportProcessedAuthorDescriptor)} method, based
 * on the SpEL expression defined in {@link NcdReportContributorConfig}.</p>
 * 
 * <p>Authors are collected in a map with the unique authors as keys, and 
 * a (potentially empty) set of duplicate authors as values. Both keys
 * and duplicate author sets maintain the order in which the authors were
 * discovered. Once all authors have been processed, this map can be retrieved
 * using the {@link #getDeduplicatedAuthors()} method.</p>
 * 
 * <p>Note that detecting whether two authors are duplicates may depend
 * on authors that will be reported later. In the following example,
 * the second author may not be detected as a duplicate of the first
 * author (depending on de-duplication expression) as both name and
 * email are different. However, the third author would be detected 
 * as a duplicate of both the first and second author, respectively based 
 * on same email and name, and hence now also the first and second author
 * should be considered duplicates. In this example, the first author
 * would be stored as the map key, and the other two authors would be
 * stored in the value set.</p>
 * <ol>
 *  <li>name: 'First Last', email: 'first.last@company.com'</li>
 *  <li>name: 'First Middle Last', email: '72431329+username@users.noreply.github.com'</li>
 *  <li>name: 'First Middle Last', email: 'first.last@company.com'</li>
 * </ol>
 * 
 * @author rsenden
 *
 */
final class NcdReportAuthorDeduplicator {
    private final Optional<Expression> dedupeExpression;
    @Getter private final ConcurrentSkipListMap<NcdReportProcessedAuthorDescriptor, Set<NcdReportProcessedAuthorDescriptor>> deduplicatedAuthors = new ConcurrentSkipListMap<>(this::compareAuthorIndex);
    
    public NcdReportAuthorDeduplicator(Optional<NcdReportContributorConfig> contributorConfig) {
        var parser = new SpelExpressionParser();
        this.dedupeExpression = contributorConfig
                .flatMap(NcdReportContributorConfig::getDuplicateExpression)
                .map(parser::parseExpression);
    }
    
    final void addAuthor(NcdReportProcessedAuthorDescriptor descriptor) {
        dedupeExpression
            .ifPresentOrElse(expr->dedupe(descriptor, expr), ()->add(descriptor));
    }
    
    /**
     * Comparator to ensure entries in deduplicatedAuthors are ordered by author number.
     */
    private final int compareAuthorIndex(NcdReportProcessedAuthorDescriptor descriptor1, NcdReportProcessedAuthorDescriptor descriptor2) {
        return Integer.compare(descriptor1.getAuthorNumber(), descriptor2.getAuthorNumber());
    }    
    
    /**
     * Add the given descriptor to deduplicatedAuthors, potentially merging multiple
     * previously identified de-duplicated authors while maintaining the order in
     * which authors were added. The unit test class contains examples that explain
     * why we are doing things this way.
     */
    private void dedupe(NcdReportProcessedAuthorDescriptor descriptor, Expression expr) {
        // Find all entries that contain a duplicate descriptor based on the given expression,
        // and merge them into the first matching entry. For example, we may have an existing 
        // entry that matches the current descriptor on name, and another matching on email,
        // so we combine these into the first entry found while removing all other entries
        // (in the reduceDuplicates method).
        var duplicateEntries = deduplicatedAuthors.entrySet().stream()
            .filter(e->containsDuplicate(e, descriptor, expr))
            .reduce(this::reduceDuplicates);
        // If we found a duplicate entry, we add the current descriptor to that entry.
        // Otherwise, we create a new entry with an empty duplicates set.
        duplicateEntries.ifPresentOrElse(
                s->s.getValue().add(descriptor),
                ()->add(descriptor));
    }

    private Entry<NcdReportProcessedAuthorDescriptor, Set<NcdReportProcessedAuthorDescriptor>> reduceDuplicates(
            Entry<NcdReportProcessedAuthorDescriptor, Set<NcdReportProcessedAuthorDescriptor>> e1,
            Entry<NcdReportProcessedAuthorDescriptor, Set<NcdReportProcessedAuthorDescriptor>> e2) {
        e1.getValue().add(e2.getKey());
        e1.getValue().addAll(deduplicatedAuthors.remove(e2.getKey())); 
        return e1;
    }

    /**
     * Returns true if a duplicate is found in the given entry (either the entry key,
     * or one of its values) for the given descriptor, based on the given expression.
     */
    private boolean containsDuplicate(Entry<NcdReportProcessedAuthorDescriptor, Set<NcdReportProcessedAuthorDescriptor>> e, NcdReportProcessedAuthorDescriptor descriptor, Expression expr) {
        var result = isDuplicate(descriptor, e.getKey(), expr)
                || e.getValue().stream().anyMatch(d->isDuplicate(descriptor, d, expr));
        return result;
    }
    
    /**
     * Returns true if the given descriptors are considered duplicates according to the
     * given expression. To avoid users having to repeat the same expression with c1 and
     * c2 reversed (for example in contains expressions), we compare both ways. 
     */
    private boolean isDuplicate(NcdReportProcessedAuthorDescriptor d1, NcdReportProcessedAuthorDescriptor d2, Expression expr) {
        return JsonHelper.evaluateSpelExpression(createCompareNode(d1, d2), expr, Boolean.class)
                || JsonHelper.evaluateSpelExpression(createCompareNode(d2, d1), expr, Boolean.class);
    }

    /**
     * Create an {@link ObjectNode} used as expression input for comparing
     * two authors 'a1' and 'a2'.
     */
    private ObjectNode createCompareNode(NcdReportProcessedAuthorDescriptor d1, NcdReportProcessedAuthorDescriptor d2) {
        var node = JsonHelper.getObjectMapper().createObjectNode();
        node.set("a1", d1.getExpressionInput());
        node.set("a2", d2.getExpressionInput());
        return node;
    }

    private final void add(NcdReportProcessedAuthorDescriptor descriptor) {
        deduplicatedAuthors.computeIfAbsent(descriptor, d->new LinkedHashSet<NcdReportProcessedAuthorDescriptor>());
    }
}