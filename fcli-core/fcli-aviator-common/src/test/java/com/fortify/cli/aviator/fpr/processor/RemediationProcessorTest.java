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
package com.fortify.cli.aviator.fpr.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric.Mode;
import com.fortify.cli.aviator.util.FprHandle;

class RemediationProcessorTest {
    @TempDir
    Path tempDir;

    @Test
    void testIssueIdFilterAppliesOnlyRequestedRemediations() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "        oldTwo();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(remediationsXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(), Set.of("ISSUE-2", "ISSUE-404"));
            var metric = processor.processRemediationXML();

            assertTrue(metric.isFiltered());
            assertEquals(Mode.FILTERED, metric.mode());
            assertEquals(2, metric.totalRemediations());
            assertEquals(1, metric.appliedRemediations());
            assertEquals(1, metric.skippedRemediations());
            assertEquals(Set.of("ISSUE-2"), metric.appliedIssueIds());
            assertEquals(Set.of("Example.java"), metric.modifiedFiles());
            assertEquals(1, metric.skippedByReason().get("Requested issue not found in remediations"));
            String updatedContent = Files.readString(sourceFile, StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertTrue(updatedContent.contains("        oldOne();"));
            assertTrue(updatedContent.contains("        newTwo();"));
            assertFalse(updatedContent.contains("        newOne();"));
        }
    }

    @Test
    void testIssueIdFilterWithNoMatchesCountsRequestedIdsAsSkipped() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-no-match"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(singleRemediationXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(), new LinkedHashSet<>(Set.of("ISSUE-404", "ISSUE-405")));
            var metric = processor.processRemediationXML();

            assertTrue(metric.isFiltered());
            assertEquals(2, metric.totalRemediations());
            assertEquals(0, metric.appliedRemediations());
            assertEquals(2, metric.skippedRemediations());
            assertEquals(Set.of(), metric.appliedIssueIds());
            assertEquals(Set.of(), metric.modifiedFiles());
            assertEquals(2, metric.skippedByReason().get("Requested issue not found in remediations"));
            assertEquals(originalContent, Files.readString(sourceFile, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testUnfilteredPathTraversalCandidateIsSkippedWithoutAborting() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-path-traversal"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(pathTraversalAndValidRemediationsXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(), null);
            var metric = processor.processRemediationXML();

            assertFalse(metric.isFiltered());
            assertEquals(Mode.UNFILTERED, metric.mode());
            assertEquals(2, metric.totalRemediations());
            assertEquals(1, metric.appliedRemediations());
            assertEquals(1, metric.skippedRemediations());
            assertEquals(1, metric.skippedByReason().get("Source file outside source directory"));
            String updatedContent = Files.readString(sourceFile, StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertTrue(updatedContent.contains("        newOne();"));
        }
    }

    @Test
    void testLoadsFvdlMetadataFromZipBackedFprPath() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-zip-backed"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        Path fprPath = createFpr(singleRemediationXml(TestHashUtil.sha256Base64Unix(originalContent)));
        Path cachePath = tempDir.resolve("remediations-cache.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(cachePath))) {
            zipOutputStream.putNextEntry(new ZipEntry("fprs/001.fpr"));
            Files.copy(fprPath, zipOutputStream);
            zipOutputStream.closeEntry();
        }

        try (FileSystem cacheFileSystem = FileSystems.newFileSystem(cachePath, (ClassLoader) null);
                FprHandle fprHandle = new FprHandle(cacheFileSystem.getPath("/fprs/001.fpr"))) {
            var metric = new RemediationProcessor(fprHandle, sourceDir.toString(), null).processRemediationXML();

            assertEquals(1, metric.appliedRemediations());
            assertEquals(Set.of("Example.java"), metric.modifiedFiles());
            assertTrue(Files.readString(sourceFile, StandardCharsets.UTF_8).contains("        newOne();"));
        }
    }

    private Path createFpr(String remediationsXml) throws IOException {
        Path fprPath = tempDir.resolve("test.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            // Encoding metadata is required by RemediationProcessor (FVDL Build/SourceFiles).
            writeEntry(zipOutputStream, "audit.fvdl", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <FVDL>
                      <Build>
                        <SourceFiles>
                          <File type="JAVA" encoding="UTF-8">
                            <Name>Example.java</Name>
                          </File>
                        </SourceFiles>
                      </Build>
                    </FVDL>
                    """);
            writeEntry(zipOutputStream, "remediations.xml", remediationsXml);
        }
        return fprPath;
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String entryName, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String remediationsXml(String hash) {
        return """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <Remediations xmlns=\"xmlns://www.fortify.com/schema/remediations\">
                    <Remediation instanceId=\"ISSUE-1\">
                        <FileChanges>
                            <Filename>Example.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>3</LineFrom>
                                <LineTo>3</LineTo>
                                <Context>    void run() {\n        oldOne();\n        oldTwo();</Context>
                                <OriginalCode>        oldOne();</OriginalCode>
                                <NewCode>        newOne();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                    <Remediation instanceId=\"ISSUE-2\">
                        <FileChanges>
                            <Filename>Example.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>4</LineFrom>
                                <LineTo>4</LineTo>
                                <Context>        oldOne();\n        oldTwo();\n    }</Context>
                                <OriginalCode>        oldTwo();</OriginalCode>
                                <NewCode>        newTwo();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                </Remediations>
                """.formatted(hash, hash);
    }

    private String singleRemediationXml(String hash) {
        return """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <Remediations xmlns=\"xmlns://www.fortify.com/schema/remediations\">
                    <Remediation instanceId=\"ISSUE-1\">
                        <FileChanges>
                            <Filename>Example.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>3</LineFrom>
                                <LineTo>3</LineTo>
                                <Context>    void run() {\n        oldOne();\n    }</Context>
                                <OriginalCode>        oldOne();</OriginalCode>
                                <NewCode>        newOne();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                </Remediations>
                """.formatted(hash);
    }

    private String pathTraversalAndValidRemediationsXml(String hash) {
        return """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <Remediations xmlns=\"xmlns://www.fortify.com/schema/remediations\">
                    <Remediation instanceId=\"ISSUE-TRAVERSAL\">
                        <FileChanges>
                            <Filename>../outside.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>3</LineFrom>
                                <LineTo>3</LineTo>
                                <Context>    void run() {\n        oldOne();\n    }</Context>
                                <OriginalCode>        oldOne();</OriginalCode>
                                <NewCode>        ignored();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                    <Remediation instanceId=\"ISSUE-1\">
                        <FileChanges>
                            <Filename>Example.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>3</LineFrom>
                                <LineTo>3</LineTo>
                                <Context>    void run() {\n        oldOne();\n    }</Context>
                                <OriginalCode>        oldOne();</OriginalCode>
                                <NewCode>        newOne();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                </Remediations>
                """.formatted(hash, hash);
    }

    @Test
    void previewModeDoesNotModifySourceFiles() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-unchanged"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "        oldTwo();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(remediationsXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(), null, true);
            var metric = processor.processRemediationXML();

            assertEquals(2, metric.totalRemediations());
            assertEquals(2, metric.appliedRemediations());
            
            // CRITICAL: Source file must be unchanged in preview mode
            String actualContent = Files.readString(sourceFile, StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertEquals(originalContent, actualContent);
            assertTrue(actualContent.contains("        oldOne();"));
            assertTrue(actualContent.contains("        oldTwo();"));
            assertFalse(actualContent.contains("newOne"));
            assertFalse(actualContent.contains("newTwo"));
        }
    }

    @Test
    void previewModePopulatesPreviewDetailsWithChanges() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-details"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(singleRemediationXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(), null, true);
            var metric = processor.processRemediationXML();

            // Verify preview details are populated
            assertTrue(metric instanceof RemediationMetric.Preview);
            var preview = (RemediationMetric.Preview) metric;
            assertEquals(1, preview.previewDetails().size());
            
            var detail = preview.previewDetails().get(0);
            assertEquals("ISSUE-1", detail.issueId());
            assertEquals("available", detail.status());
            assertNotNull(detail.files());
            assertEquals(1, detail.files().size());
            
            var filePreview = detail.files().get("Example.java");
            assertNotNull(filePreview);
            assertEquals("UTF-8", filePreview.encoding());
            assertEquals(1, filePreview.changes().size());
            
            var change = filePreview.changes().get(0);
            assertEquals(1, change.changeIndex());
            assertEquals(3, change.lineFrom());
            assertEquals(3, change.lineTo());
            assertTrue(change.originalCode().contains("oldOne"));
            assertTrue(change.newCode().contains("newOne"));
        }
    }

    @Test
    void previewModeCapturesSkipReasonsInPreviewDetails() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-skip"));
        // Create file for one remediation, but not the other
        Path validFile = sourceDir.resolve("Valid.java");
        Files.writeString(validFile, "class Valid { void run() { old(); } }", StandardCharsets.UTF_8);

        String validHash = TestHashUtil.sha256Base64Unix("class Valid { void run() { old(); } }");
        String missingHash = "dGVzdGhhc2g="; // arbitrary hash for missing file
        
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Remediations xmlns="xmlns://www.fortify.com/schema/remediations">
                    <Remediation instanceId="ISSUE-VALID">
                        <FileChanges>
                            <Filename>Valid.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>1</LineFrom>
                                <LineTo>1</LineTo>
                                <Context>class Valid { void run() { old(); } }</Context>
                                <OriginalCode>old();</OriginalCode>
                                <NewCode>new();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                    <Remediation instanceId="ISSUE-MISSING">
                        <FileChanges>
                            <Filename>Missing.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>1</LineFrom>
                                <LineTo>1</LineTo>
                                <Context>ignored</Context>
                                <OriginalCode>ignored</OriginalCode>
                                <NewCode>ignored</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                </Remediations>
                """.formatted(validHash, missingHash);

        Path fprPath = tempDir.resolve("test-skip.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zipOutputStream, "audit.fvdl", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <FVDL>
                      <Build>
                        <SourceFiles>
                          <File type="JAVA" encoding="UTF-8">
                            <Name>Valid.java</Name>
                          </File>
                          <File type="JAVA" encoding="UTF-8">
                            <Name>Missing.java</Name>
                          </File>
                        </SourceFiles>
                      </Build>
                    </FVDL>
                    """);
            writeEntry(zipOutputStream, "remediations.xml", xml);
        }

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(), null, true);
            var metric = processor.processRemediationXML();

            assertEquals(2, metric.totalRemediations());
            assertEquals(1, metric.appliedRemediations());
            assertEquals(1, metric.skippedRemediations());
            
            // Verify preview details capture both available and skipped
            assertTrue(metric instanceof RemediationMetric.Preview);
            var preview = (RemediationMetric.Preview) metric;
            assertEquals(2, preview.previewDetails().size());
            
            var available = preview.previewDetails().stream()
                    .filter(d -> "available".equals(d.status()))
                    .findFirst().orElseThrow();
            assertEquals("ISSUE-VALID", available.issueId());
            assertNotNull(available.files().get("Valid.java"));
            
            var skipped = preview.previewDetails().stream()
                    .filter(d -> "skipped".equals(d.status()))
                    .findFirst().orElseThrow();
            assertEquals("ISSUE-MISSING", skipped.issueId());
            assertEquals("Source file missing", skipped.skipReason());
            assertTrue(skipped.files().isEmpty());
        }
    }

    @Test
    void previewModeWithEmptyRemediationsXmlReturnsEmptyList() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-empty"));
        Path sourceFile = sourceDir.resolve("Example.java");
        Files.writeString(sourceFile, "class Example { }", StandardCharsets.UTF_8);

        String emptyXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Remediations xmlns="xmlns://www.fortify.com/schema/remediations">
                </Remediations>
                """;

        Path fprPath = tempDir.resolve("test-empty.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zipOutputStream, "audit.fvdl", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <FVDL>
                      <Build>
                        <SourceFiles>
                          <File type="JAVA" encoding="UTF-8">
                            <Name>Example.java</Name>
                          </File>
                        </SourceFiles>
                      </Build>
                    </FVDL>
                    """);
            writeEntry(zipOutputStream, "remediations.xml", emptyXml);
        }

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(), null, true);
            var metric = processor.processRemediationXML();

            assertEquals(0, metric.totalRemediations());
            assertEquals(0, metric.appliedRemediations());
            
            // CRITICAL: Must return empty list [], not null
            assertTrue(metric instanceof RemediationMetric.Preview);
            var preview = (RemediationMetric.Preview) metric;
            assertEquals(0, preview.previewDetails().size());
            assertEquals(java.util.List.of(), preview.previewDetails());
        }
    }

    private static final class TestHashUtil {
        private static String sha256Base64Unix(String content) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(content.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8));
                return java.util.Base64.getEncoder().encodeToString(digest);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}