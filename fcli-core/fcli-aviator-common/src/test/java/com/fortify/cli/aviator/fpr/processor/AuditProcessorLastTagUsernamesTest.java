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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;

class AuditProcessorLastTagUsernamesTest {
    private static final String INSTANCE_ID = "ISSUE-1";

    @TempDir
    Path tempDir;

    private FprHandle fprHandle;

    @AfterEach
    void tearDown() throws Exception {
        if (fprHandle != null) {
            fprHandle.close();
        }
    }

    @Test
    void lastDocumentOrderTagHistoryWinsPerTag() throws Exception {
        fprHandle = new FprHandle(createTestFpr("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="xmlns://www.fortify.com/schema/audit" version="4.4">
                  <ns2:IssueList>
                    <ns2:Issue instanceId="ISSUE-1" revision="0">
                      <ns2:Tag id="%s">
                        <ns2:Value>Exploitable</ns2:Value>
                      </ns2:Tag>
                      <ns2:Tag id="%s">
                        <ns2:Value>Exploitable</ns2:Value>
                      </ns2:Tag>
                      <ns2:TagHistory>
                        <ns2:Tag id="%s">
                          <ns2:Value>Not an Issue</ns2:Value>
                        </ns2:Tag>
                        <ns2:Username>%s</ns2:Username>
                      </ns2:TagHistory>
                      <ns2:TagHistory>
                        <ns2:Tag id="%s">
                          <ns2:Value>Exploitable</ns2:Value>
                        </ns2:Tag>
                        <ns2:Username>analyst.user</ns2:Username>
                      </ns2:TagHistory>
                      <ns2:TagHistory>
                        <ns2:Tag id="%s">
                          <ns2:Value>Exploitable</ns2:Value>
                        </ns2:Tag>
                        <ns2:Username>%s</ns2:Username>
                      </ns2:TagHistory>
                    </ns2:Issue>
                  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(
                Constants.ANALYSIS_TAG_ID, Constants.AUDITOR_STATUS_TAG_ID,
                Constants.ANALYSIS_TAG_ID, Constants.USER_NAME,
                Constants.ANALYSIS_TAG_ID,
                Constants.AUDITOR_STATUS_TAG_ID, Constants.USER_NAME_LEGACY_FORTIFY_AVIATOR)));

        Map<String, AuditIssue> issues = new AuditProcessor(fprHandle).processAuditXML();
        Map<String, String> lastTagUsernames = issues.get(INSTANCE_ID).getLastTagUsernames();

        assertEquals("analyst.user", lastTagUsernames.get(Constants.ANALYSIS_TAG_ID));
        assertEquals(Constants.USER_NAME_LEGACY_FORTIFY_AVIATOR, lastTagUsernames.get(Constants.AUDITOR_STATUS_TAG_ID));
    }

    @Test
    void missingUsernameIsStoredAsEmptyString() throws Exception {
        fprHandle = new FprHandle(createTestFpr("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="xmlns://www.fortify.com/schema/audit" version="4.4">
                  <ns2:IssueList>
                    <ns2:Issue instanceId="ISSUE-1" revision="0">
                      <ns2:TagHistory>
                        <ns2:Tag id="%s">
                          <ns2:Value>Exploitable</ns2:Value>
                        </ns2:Tag>
                      </ns2:TagHistory>
                    </ns2:Issue>
                  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(Constants.ANALYSIS_TAG_ID)));

        Map<String, AuditIssue> issues = new AuditProcessor(fprHandle).processAuditXML();

        assertEquals("", issues.get(INSTANCE_ID).getLastTagUsernames().get(Constants.ANALYSIS_TAG_ID));
    }

    private Path createTestFpr(String auditXml) throws Exception {
        Path fprPath = Files.createTempFile(tempDir, "audit-processor", ".fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("audit.xml"));
            zipOutputStream.write(auditXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("src-archive/index.xml"));
            zipOutputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><index/>".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }
}
