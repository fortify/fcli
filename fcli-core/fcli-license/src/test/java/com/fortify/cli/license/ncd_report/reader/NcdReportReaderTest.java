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
package com.fortify.cli.license.ncd_report.reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NcdReportReaderTest {
    @TempDir private Path tempDir;

    @Test
    void readConfigIgnoresUnknownYamlProperties() throws IOException {
        writeReportConfig("""
            unknownTopLevel: keep-forward-compatible
            sources:
              includeForks: true
              unknownSourcesProperty: ignored
              github: []
            contributor:
              duplicateExpression: a1.name==a2.name
              unknownContributorProperty: ignored
            """);

        try ( var reader = new NcdReportReader(tempDir) ) {
            var config = reader.readConfig();
            Assertions.assertNotNull(config, "Expected config to be loaded");
            Assertions.assertTrue(config.getContributor().isPresent(), "Expected contributor config to be present");
            Assertions.assertEquals(
                "a1.name==a2.name",
                config.getContributor().get().getDuplicateExpression().orElse(null),
                "Expected known contributor property to be deserialized"
            );
        }
    }

    @Test
    void readContributorsAllowsExtraCsvColumns() throws IOException {
        writeReportConfig("sources: {}\n");
        writeContributorsCsv(List.of(
            "authorName,authorEmail,authorState,contributionStatus,unexpectedColumn",
            "Alice,alice@example.com,processed,contributing,extra"
        ));

        try ( var reader = new NcdReportReader(tempDir) ) {
            var contributors = reader.readContributorsAsObjectNodeStream().toList();
            Assertions.assertEquals(1, contributors.size(), "Expected one contributor row");
            var contributor = contributors.get(0);
            Assertions.assertEquals("Alice", contributor.path("authorName").asText(), "Expected known field to be read");
            Assertions.assertEquals("extra", contributor.path("unexpectedColumn").asText(), "Expected extra field to be preserved");
            Assertions.assertTrue(contributor.path("authorId").isTextual(), "Expected normalizer to populate authorId");
        }
    }

    private void writeReportConfig(String content) throws IOException {
        Files.writeString(tempDir.resolve("report-config.yaml"), content);
    }

    private void writeContributorsCsv(List<String> lines) throws IOException {
        Files.write(tempDir.resolve("contributors.csv"), lines);
    }
}
