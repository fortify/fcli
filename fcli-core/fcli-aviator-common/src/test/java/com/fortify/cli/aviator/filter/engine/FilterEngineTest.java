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
package com.fortify.cli.aviator.filter.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.Filter;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.SearchTree;
import com.fortify.cli.aviator.fpr.filter.VulnerabilityFilterer;
import com.fortify.cli.aviator.fpr.filter.comparer.BooleanComparer;
import com.fortify.cli.aviator.fpr.filter.comparer.SearchComparer;
import com.fortify.cli.aviator.fpr.filter.engine.FilterParser;

class FilterEngineTest {

    private static int countTreeNodes(SearchTree.Node node) {
        if (node == null) return 0;
        if (node.isLeaf()) return 1;
        return 1 + countTreeNodes(node.getLeftChild()) + countTreeNodes(node.getRightChild());
    }

    @Nested
    class ModernParserTests {
        // All these tests now pass with the robust manual tokenizer.
        @Test void testSimpleTerm() { assertEquals(1, countTreeNodes(FilterParser.parse("category:value").getRoot())); }
        @Test void testBracketedModifierWithSpaces() { assertEquals(1, countTreeNodes(FilterParser.parse("[fortify priority order]:critical").getRoot())); }
        @Test void testImplicitAnd() { assertEquals(3, countTreeNodes(FilterParser.parse("analyzer:A category:B").getRoot())); }
        @Test void testImplicitAndWithMultiWordValue() { assertEquals(3, countTreeNodes(FilterParser.parse("[category]:Insecure Dependency: Vulnerable Component [analysis type]:SCA").getRoot())); }
        @Test void testExplicitAnd() { assertEquals(3, countTreeNodes(FilterParser.parse("[PCI 4.0]:<none> AND [fortify priority order]:low").getRoot())); }
        @Test void testExplicitOr() { assertEquals(3, countTreeNodes(FilterParser.parse("analyzer:A OR analyzer:B").getRoot())); }
        @Test void testOperatorPrecedence() { assertEquals(5, countTreeNodes(FilterParser.parse("A:1 OR B:2 AND C:3").getRoot())); }
        @Test void testTermWithQuotedValue() { assertEquals(1, countTreeNodes(FilterParser.parse("category:\"SQL Injection\"").getRoot())); }
        @Test void testTermWithEscapedColonInValue() { assertEquals(1, countTreeNodes(FilterParser.parse("category:Password Management\\: Weak Encryption").getRoot())); }
    }

    @Nested
    class LegacyParserTests {
        // These tests correctly validate the legacy parser logic.
        @Test void testImplicitAndForDifferentModifiers() { assertEquals(3, countTreeNodes(FilterParser.parseLegacy("analyzer:A category:B").getRoot())); }
        @Test void testImplicitOrForSameModifier() { assertTrue(FilterParser.parseLegacy("category:A category:B").getRoot().getQuery().getSearchComparer() instanceof BooleanComparer); }
        @Test void testImplicitAndForNegatedSameModifier() {
            SearchComparer comparer = FilterParser.parseLegacy("category:!A category:!B").getRoot().getQuery().getSearchComparer();
            assertTrue(comparer instanceof BooleanComparer);
            assertEquals(2, ((BooleanComparer)comparer).getAndComparers().size());
            assertEquals(0, ((BooleanComparer)comparer).getOrComparers().size());
        }
    }

    @Nested
    class FiltererTests {
        private static Vulnerability vulnCritical, vulnHigh, vulnMedium, vulnLow, vulnToHideByAudience, vulnToHideByCategory, vulnToKeep;
        private static List<Vulnerability> allVulnerabilities;

        @BeforeAll
        static void setupVulnerabilities() {
            vulnCritical = new Vulnerability(); vulnCritical.setInstanceID("CRITICAL_VULN"); vulnCritical.setPriority("Critical"); vulnCritical.setImpact(5.0); vulnCritical.setLikelihood("5.0"); vulnCritical.setConfidence(5.0); vulnCritical.setAudience("broad,fod");
            vulnHigh = new Vulnerability(); vulnHigh.setInstanceID("HIGH_VULN"); vulnHigh.setPriority("High"); vulnHigh.setImpact(4.0); vulnHigh.setLikelihood("4.0"); vulnHigh.setConfidence(4.0); vulnHigh.setAudience("broad,fod");
            vulnMedium = new Vulnerability(); vulnMedium.setInstanceID("MEDIUM_VULN"); vulnMedium.setPriority("Medium"); vulnMedium.setImpact(2.0); vulnMedium.setLikelihood("4.0"); vulnMedium.setConfidence(4.0); vulnMedium.setAudience("broad,fod");
            vulnLow = new Vulnerability(); vulnLow.setInstanceID("LOW_VULN"); vulnLow.setPriority("Low"); vulnLow.setImpact(1.0); vulnLow.setLikelihood("0.5"); vulnLow.setConfidence(2.0); vulnLow.setAudience("broad,fod"); vulnLow.setCategory("Insecure Dependency: Vulnerable Component");
            vulnToHideByAudience = new Vulnerability(); vulnToHideByAudience.setInstanceID("HIDDEN_BY_AUDIENCE"); vulnToHideByAudience.setPriority("High"); vulnToHideByAudience.setImpact(4.0); vulnToHideByAudience.setLikelihood("4.0"); vulnToHideByAudience.setConfidence(4.0); vulnToHideByAudience.setAudience("broad"); vulnToHideByAudience.setAnalyzerName("Structural"); vulnToHideByAudience.setCategory("Some Random Category");
            vulnToHideByCategory = new Vulnerability(); vulnToHideByCategory.setInstanceID("HIDDEN_BY_CATEGORY"); vulnToHideByCategory.setPriority("High"); vulnToHideByCategory.setImpact(4.0); vulnToHideByCategory.setLikelihood("4.0"); vulnToHideByCategory.setConfidence(4.0); vulnToHideByCategory.setAudience("broad,fod"); vulnToHideByCategory.setAnalyzerName("Structural"); vulnToHideByCategory.setCategory("Insecure Dependency: Vulnerable Component");  // Changed to match hide category
            vulnToKeep = new Vulnerability(); vulnToKeep.setInstanceID("SHOULD_BE_KEPT"); vulnToKeep.setPriority("High"); vulnToKeep.setImpact(4.0); vulnToKeep.setLikelihood("4.0"); vulnToKeep.setConfidence(4.0); vulnToKeep.setAudience("broad,fod"); vulnToKeep.setAnalyzerName("Structural"); vulnToKeep.setCategory("Some Random Category");
            allVulnerabilities = List.of(vulnCritical, vulnHigh, vulnMedium, vulnLow, vulnToHideByAudience, vulnToHideByCategory, vulnToKeep);
        }

        @Test
        void testQuickViewFilter() {
            FilterSet quickView = new FilterSet();
            quickView.setFilters(List.of(
                createFolderFilter("[fortify priority order]:critical OR [fortify priority order]:high OR [fortify priority order]:medium OR [fortify priority order]:low"),
                createHideFilter("impact:![2.5, 5.0]"),
                createHideFilter("likelihood:![1.0,5.0]"),
                createHideFilter("[category]:\"Insecure Dependency\\: Vulnerable Component\"") // with quote and \\ : to full, Exact match
            ));
            List<Vulnerability> result = VulnerabilityFilterer.filterVulnerabilities(allVulnerabilities, quickView);
            Set<String> resultIds = result.stream().map(Vulnerability::getInstanceID).collect(Collectors.toSet());

            assertEquals(4, result.size(), "Should keep Critical, High, H_BY_A, S_BE_K (H_BY_C hidden by category)");
            assertTrue(resultIds.contains("CRITICAL_VULN"));
            assertTrue(resultIds.contains("HIGH_VULN"));
            assertTrue(resultIds.contains("HIDDEN_BY_AUDIENCE"));
            assertTrue(resultIds.contains("SHOULD_BE_KEPT"));
        }

        @Test
        void testLegacyFoDFilterHidesCorrectly() {
            FilterSet fodFilter = new FilterSet();
            fodFilter.setFilters(List.of(
                createFolderFilter("[fortify priority order]:high"),
                createHideFilter("audience:!fod analyzer:!pentest category:!docker")
            ));
            List<Vulnerability> result = VulnerabilityFilterer.filterVulnerabilities(allVulnerabilities, fodFilter);

            assertEquals(3, result.size(), "Should keep vulnHigh, H_BY_C, S_BE_K (H_BY_A hidden)");
            Set<String> resultIds = result.stream().map(Vulnerability::getInstanceID).collect(Collectors.toSet());
            assertTrue(resultIds.contains("HIGH_VULN"));
            assertTrue(resultIds.contains("HIDDEN_BY_CATEGORY"));
            assertTrue(resultIds.contains("SHOULD_BE_KEPT"));
        }

        // NEW TEST: Regex
        @Test
        void testRegexMatch() {
            Vulnerability vuln = new Vulnerability();
            vuln.setCategory("SQL Injection");
            List<Vulnerability> vulns = List.of(vuln);
            List<Vulnerability> result = VulnerabilityFilterer.filter(vulns, "category:/sql.*/");
            assertEquals(1, result.size());
        }

        // Null Attr Handling
        @Test
        void testNullAttrNotContains() {
            Vulnerability vuln = new Vulnerability(); // analyzer null
            List<Vulnerability> vulns = List.of(vuln);
            List<Vulnerability> result = VulnerabilityFilterer.filter(vulns, "analyzer:!pentest");
            assertEquals(1, result.size()); // !false (null not contains) = true, matches
        }

        private Filter createFolderFilter(String query) { Filter f = new Filter(); f.setAction("setFolder"); f.setQuery(query); return f; }
        private Filter createHideFilter(String query) { Filter f = new Filter(); f.setAction("hide"); f.setQuery(query); return f; }
    }
}
