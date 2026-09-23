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
package com.fortify.cli.aviator.ssc.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.grpc.CorrelatedPair;
import com.fortify.cli.aviator.util.FprHandle;

class DastFprCorrelationEnricherTest {
    @TempDir Path tempDir;

    @Test
    void preservesExistingExternalFindingsAndAvoidsDuplicates() throws Exception {
        Path fprPath = createFpr();
        var pairs = List.of(
            new CorrelatedPair("SAST-1", "DAST-1", "scan-1", "HIGH", "existing"),
            new CorrelatedPair("SAST-2", "DAST-1", "scan-2", "HIGH", "new")
        );

        new DastFprCorrelationEnricher().injectAndRepackage(fprPath, pairs);

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var factory = DocumentBuilderFactory.newInstance();
          try (var inputStream = Files.newInputStream(fprHandle.getPath("/webinspect.xml"))) {
            var document = factory.newDocumentBuilder().parse(inputStream);
            var originFindingIds = document.getElementsByTagName("OriginFindingID");
            assertEquals(2, originFindingIds.getLength());
            assertEquals("SAST-1", originFindingIds.item(0).getTextContent());
            assertEquals("SAST-2", originFindingIds.item(1).getTextContent());
          }
        }
    }

    private Path createFpr() throws Exception {
        Path fprPath = tempDir.resolve("merged.fpr");
        try (FileSystem zipFs = FileSystems.newFileSystem(fprPath, Map.of("create", "true"))) {
            Files.writeString(zipFs.getPath("/webinspect.xml"), """
                <ScanResults>
                  <Session requestId="REQ-1">
                    <Issues>
                      <Issue id="DAST-1">
                        <ExternalFindings>
                          <ExternalFinding Origin="SCA">
                            <OriginID>scan-1</OriginID>
                            <OriginFindingID>SAST-1</OriginFindingID>
                            <OriginDateTime>2026-01-01T00:00:00Z</OriginDateTime>
                          </ExternalFinding>
                        </ExternalFindings>
                      </Issue>
                    </Issues>
                  </Session>
                </ScanResults>
                """, StandardCharsets.UTF_8);
        }
        return fprPath;
    }
}