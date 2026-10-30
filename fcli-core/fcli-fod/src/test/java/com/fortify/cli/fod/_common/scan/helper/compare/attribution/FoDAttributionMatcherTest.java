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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

import static com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionEngineTest.issue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionUrlChangeIndex.SiteTreeUrl;

class FoDAttributionMatcherTest {
    @Test
    void urlNormalization() {
        var url = FoDAttributionUrls.normalize("http://Zero.WebAppSecurity.com:80/resources/js/bootstrap.min.js\tHTTP/1.1/../../x");
        assertEquals("http://zero.webappsecurity.com", url.origin());
        assertEquals("/resources/js/bootstrap.min.js", url.path());
        assertEquals("http://zero.webappsecurity.com/admin", FoDAttributionUrls.normalizeKey("http://zero.webappsecurity.com/admin/"));
        assertEquals("/<script>alert('TRACK');</script>", FoDAttributionUrls.normalize("/%3Cscript%3Ealert('TRACK');%3C/script%3E").path());
        assertEquals("/search.html?a=1+2", FoDAttributionUrls.normalize("/search.html?a=1+2").path());
        assertEquals("/", FoDAttributionUrls.normalize("https://host:443").path());
        assertEquals("https://host:8443/", FoDAttributionUrls.normalizeKey("https://host:8443/"));
        assertNull(FoDAttributionUrls.normalize("  "));
    }

    @Test
    void fileIndexMatchesBySuffixAndDetectsChanges() {
        var previous = manifest("/Src/app/A.java", "10", "/Src/app/B.java", "20", "/Src/old/C.java", "30", "/Src/app/D.java", "40");
        var current = manifest("/Src/app/A.java", "10", "/Src/app/B.java", "21", "/Src/new/C.java", "30", "/Src/app/E.java", "50");
        var index = new FoDAttributionFileChangeIndex(previous, current);
        assertEquals(FoDAttributionFileChangeIndex.Kind.UNCHANGED, index.lookup("app/A.java").get(0).kind());
        assertEquals(FoDAttributionFileChangeIndex.Kind.CHANGED, index.lookup("app\\B.java").get(0).kind());
        assertEquals(FoDAttributionFileChangeIndex.Kind.REMOVED, index.lookup("D.java").get(0).kind());
        assertEquals(FoDAttributionFileChangeIndex.Kind.ADDED, index.lookup("/src/app/e.java").get(0).kind());
        var moved = index.lookup("old/C.java").get(0);
        assertEquals(FoDAttributionFileChangeIndex.Kind.MOVED, moved.kind());
        assertEquals("/Src/new/C.java", moved.movedTo());
        assertEquals(List.of("/Src/new/C.java", "/Src/app/E.java"), index.getAddedPaths());
        assertEquals(List.of("/Src/old/C.java", "/Src/app/D.java"), index.getRemovedPaths());
        assertEquals(List.of("/Src/app/B.java"), index.getChangedPaths());
        assertTrue(index.lookup("pp/A.java").isEmpty());
    }

    @Test
    void fileMatcherProducesEvidenceAndReferencesChanges() {
        var index = new FoDAttributionFileChangeIndex(
                manifest("/Src/a/util.py", "1", "/Src/b/util.py", "2"),
                manifest("/Src/a/util.py", "1", "/Src/b/util.py", "3", "/Src/c/new.py", "4"));
        var context = FoDAttributionContext.builder().changeIndex(index).build();
        var matcher = new FoDAttributionFileChangeMatcher();
        var added = matcher.match(context, issue("c/new.py", "XSS", FoDAttributionTransition.NEW));
        assertEquals(FoDAttributionCause.FILE_ADDED, added.get(0).cause());
        assertEquals("file /Src/c/new.py was added", added.get(0).evidence());
        var ambiguous = matcher.match(context, issue("util.py", "XSS", FoDAttributionTransition.NEW));
        assertEquals(FoDAttributionCause.FILE_MATCH_AMBIGUOUS, ambiguous.get(0).cause());
        assertTrue(matcher.match(context, issue("a/util.py", "XSS", FoDAttributionTransition.NEW)).isEmpty());
        assertEquals(Set.of("/Src/c/new.py", "/Src/b/util.py"), context.getReferencedChanges());
    }

    @Test
    void urlMatcherUsesRequestMethodForChangedUrls() {
        // From the zero.webappsecurity.com sample: OPTIONS method no longer observed for the root URL
        var index = new FoDAttributionUrlChangeIndex(
                List.of(url("http://zero.webappsecurity.com/", Set.of("GET", "OPTIONS"), Set.of("200")),
                        url("http://zero.webappsecurity.com/admin/users.html", Set.of("GET"), Set.of("200"))),
                List.of(url("http://zero.webappsecurity.com/", Set.of("GET"), Set.of("200")),
                        url("http://zero.webappsecurity.com/debug.dat", Set.of("GET"), Set.of("200"))));
        var lookup = new StubLookup(Map.of(
                "OPTIONS", new IFoDIssueRequestLookup.RequestInfo("OPTIONS", "200"),
                "Autocomplete", new IFoDIssueRequestLookup.RequestInfo("GET", "200")));
        var context = FoDAttributionContext.builder().changeIndex(index).requestLookup(lookup).build();
        var matcher = new FoDAttributionUrlChangeMatcher();
        var options = matcher.match(context, issue("http://zero.webappsecurity.com:80/", "OPTIONS", FoDAttributionTransition.FIXED));
        assertEquals(FoDAttributionCause.URL_METHOD_CHANGED, options.get(0).cause());
        assertTrue(matcher.match(context, issue("http://zero.webappsecurity.com:80/", "Autocomplete", FoDAttributionTransition.FIXED)).isEmpty());
        var removed = matcher.match(context, issue("http://zero.webappsecurity.com:80/admin/users.html", "SSN", FoDAttributionTransition.FIXED));
        assertEquals(FoDAttributionCause.URL_REMOVED, removed.get(0).cause());
        var added = matcher.match(context, issue("/debug.dat", "Unprotected File", FoDAttributionTransition.NEW));
        assertEquals(FoDAttributionCause.URL_ADDED, added.get(0).cause());
        var related = matcher.match(context, issue("http://zero.webappsecurity.com/admin/index.html", "X", FoDAttributionTransition.FIXED));
        assertEquals(FoDAttributionCause.RELATED_URL_CHANGED, related.get(0).cause());
        assertTrue(matcher.match(context, issue("http://zero.webappsecurity.com/index.html", "X", FoDAttributionTransition.FIXED)).isEmpty());
    }

    @Test
    void urlMatcherWithoutRequestLookupReportsPossibleChange() {
        var index = new FoDAttributionUrlChangeIndex(
                List.of(url("http://host/a", Set.of("GET", "OPTIONS"), Set.of("200"))),
                List.of(url("http://host/a", Set.of("GET"), Set.of("200"))));
        var context = FoDAttributionContext.builder().changeIndex(index).build();
        var evidence = new FoDAttributionUrlChangeMatcher().match(context, issue("http://host/a", "X", FoDAttributionTransition.FIXED));
        assertEquals(FoDAttributionCause.URL_CHANGED, evidence.get(0).cause());
    }

    @Test
    void targetHostMatcher() {
        var previous = JsonHelper.getObjectMapper().createObjectNode();
        previous.putObject("d").put("url", "http://old.example.com/");
        var current = JsonHelper.getObjectMapper().createObjectNode();
        current.putObject("d").put("url", "https://new.example.com/");
        var context = FoDAttributionContext.builder().previousSummary(previous).currentSummary(current).build();
        var matcher = new FoDAttributionTargetHostMatcher("/d/url");
        assertEquals(1, matcher.match(context, issue("http://old.example.com:80/a", "X", FoDAttributionTransition.FIXED)).size());
        assertEquals(1, matcher.match(context, issue("https://new.example.com/a", "X", FoDAttributionTransition.NEW)).size());
        assertTrue(matcher.match(context, issue("https://new.example.com/a", "X", FoDAttributionTransition.FIXED)).isEmpty());
    }

    private static Map<String, String> manifest(String... pathsAndFingerprints) {
        var result = new LinkedHashMap<String, String>();
        for ( int i = 0; i < pathsAndFingerprints.length; i += 2 ) { result.put(pathsAndFingerprints[i], pathsAndFingerprints[i + 1]); }
        return result;
    }

    private static SiteTreeUrl url(String url, Set<String> methods, Set<String> statusCodes) {
        return new SiteTreeUrl(url, new TreeSet<>(methods), new TreeSet<>(statusCodes));
    }

    private static final class StubLookup implements IFoDIssueRequestLookup {
        private final Map<String, RequestInfo> byCategory;
        private int count;

        StubLookup(Map<String, RequestInfo> byCategory) {
            this.byCategory = byCategory;
        }

        @Override
        public RequestInfo lookup(FoDAttributionIssue issue) {
            count++;
            return byCategory.get(issue.getCategory());
        }

        @Override
        public int getLookupCount() {
            return count;
        }

        @Override
        public boolean isLimitReached() {
            return false;
        }
    }
}
