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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.util.ncd_report.config.NcdReportContributorConfig;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportAuthorDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor.NcdReportProcessedAuthorState;

import lombok.Data;
import lombok.RequiredArgsConstructor;

public class NcdReportAuthorDeduplicatorTest {
    @Test
    public void testEmptyConfig() {
        var descriptors = getDescriptors();
        var deduplicatedAuthors = getDeduplicatedAuthors(getEmptyConfig(), descriptors);
        Assertions.assertEquals(descriptors.length, deduplicatedAuthors.size(), "Incorrect size");
        var numbers = deduplicatedAuthors.keySet().stream()
            .map(NcdReportProcessedAuthorDescriptor::getAuthorNumber)
            .toArray(Integer[]::new);
        Assertions.assertArrayEquals(new Integer[] {1,2,3,4}, numbers, "Incorrect number order");
        deduplicatedAuthors.values().forEach(s->Assertions.assertTrue(s.isEmpty(), "Duplicates set should be empty"));
    }
    
    @Test
    public void testDedupeConfig() {
        var descriptors = getDescriptors();
        var deduplicatedAuthors = getDeduplicatedAuthors(getDedupeConfig(), descriptors);
        Assertions.assertEquals(1, deduplicatedAuthors.size(), "Incorrect size");
        Assertions.assertEquals(1, deduplicatedAuthors.keySet().stream().findFirst().get().getAuthorNumber(), "Invalid number for original record");
        var numbers = deduplicatedAuthors.values().stream().findFirst().get().stream()
                .map(NcdReportProcessedAuthorDescriptor::getAuthorNumber)
                .toArray(Integer[]::new);
        Assertions.assertArrayEquals(new Integer[] {2,3,4}, numbers, "Incorrect number order");
        deduplicatedAuthors.values().forEach(s->Assertions.assertEquals(descriptors.length-1, s.size(), "Unexpected size for duplicates set"));
    }
    
    private Map<NcdReportProcessedAuthorDescriptor, Set<NcdReportProcessedAuthorDescriptor>> getDeduplicatedAuthors(Optional<NcdReportContributorConfig> config, NcdReportProcessedAuthorDescriptor[] descriptors) {
        NcdReportAuthorDeduplicator deduplicator = new NcdReportAuthorDeduplicator(config);
        Stream.of(descriptors).forEach(deduplicator::addAuthor);
        return deduplicator.getDeduplicatedAuthors();
    }

    private NcdReportProcessedAuthorDescriptor[] getDescriptors() {
        return new NcdReportProcessedAuthorDescriptor[]{
                // Initial entry
                get(new AuthorDescriptor("Ruud Senden","8635138+rsenden@users.noreply.github.com"), 1),
                // New entry, no direct duplicate of #1
                get(new AuthorDescriptor("rsenden","rsenden@users.noreply.github.com"), 2),
                // Duplicate of #2 due to same name
                get(new AuthorDescriptor("rsenden","ruud.senden@microfocus.com"), 3),
                // Duplicate of #1 (on name) and #3 (on email), so all should now be considered duplicates
                get(new AuthorDescriptor("Ruud Senden","ruud.senden@microfocus.com"), 4)};
    }
    
    private Optional<NcdReportContributorConfig> getEmptyConfig() {
        return Optional.empty();
    }
    
    private Optional<NcdReportContributorConfig> getDedupeConfig() {
        NcdReportContributorConfig config = new NcdReportContributorConfig();
        config.setDuplicateExpression(Optional.of("a1.name==a2.name || a1.email==a2.email"));
        return Optional.of(config);
    }

    private final NcdReportProcessedAuthorDescriptor get(AuthorDescriptor descriptor, int i) {
        return new NcdReportProcessedAuthorDescriptor(descriptor, NcdReportProcessedAuthorState.processed, i, JsonHelper.getObjectMapper().valueToTree(descriptor));
    }
    
    @RequiredArgsConstructor @Data
    private final class AuthorDescriptor implements INcdReportAuthorDescriptor {
        private final String name;
        private final String email;
    }
}
