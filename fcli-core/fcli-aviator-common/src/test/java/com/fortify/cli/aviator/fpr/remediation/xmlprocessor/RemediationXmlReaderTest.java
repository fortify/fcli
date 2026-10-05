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
package com.fortify.cli.aviator.fpr.remediation.xmlprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemediationXmlReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void doesNotResolveExternalEntity() throws Exception {
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "PRIVATE-SENTINEL");
        Path remediations = tempDir.resolve("remediations.xml");
        Files.writeString(remediations, """
            <!DOCTYPE Remediations [<!ENTITY xxe SYSTEM "%s">]>
            <Remediations><Comment>&xxe;</Comment></Remediations>
            """.formatted(secret.toUri()));

        var document = new RemediationXmlReader().read(remediations);
        assertEquals("", document.getElementsByTagName("Comment").item(0).getTextContent());
    }

    @Test
    void allowsDoctypeWithoutLoadingExternalDtd() throws Exception {
        Path remediations = tempDir.resolve("remediations.xml");
        Files.writeString(remediations, """
            <!DOCTYPE Remediations SYSTEM "http://example.invalid/remediations.dtd">
            <Remediations><Comment>expected</Comment></Remediations>
            """);

        var document = new RemediationXmlReader().read(remediations);
        assertEquals("expected", document.getElementsByTagName("Comment").item(0).getTextContent());
    }
}