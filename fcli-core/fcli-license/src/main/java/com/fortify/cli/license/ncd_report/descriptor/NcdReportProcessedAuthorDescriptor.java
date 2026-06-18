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
package com.fortify.cli.license.ncd_report.descriptor;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliBugException;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data
public class NcdReportProcessedAuthorDescriptor {
    private final INcdReportAuthorDescriptor authorDescriptor;
    private final NcdReportProcessedAuthorState state;
    private final int authorNumber;
    private final ObjectNode expressionInput;
    
    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return objectNode.put("authorId", computeAuthorId())
                .put("authorName", authorDescriptor.getName())
                .put("authorEmail", authorDescriptor.getEmail())
                .put("authorState", state.name())
                .put("authorNumber", authorNumber);
    }

    /**
     * Computes a stable 16-hex-char identifier for this author derived from the
     * normalised cleanName and cleanEmailName fields already present in the
     * expression input.  The value is reproducible across separate runs as long
     * as the author's name/email are the same, making it suitable as a stable
     * cross-report reference key (e.g. for AI-assisted deduplication annotations).
     */
    private String computeAuthorId() {
        var cleanName      = expressionInput.path("cleanName").asText("");
        var cleanEmailName = expressionInput.path("cleanEmailName").asText("");
        var input = cleanName + ":" + cleanEmailName;
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return String.format("%032x", new BigInteger(1, hash)).substring(0, 16);
        } catch ( NoSuchAlgorithmException e ) {
            throw new FcliBugException("SHA-256 not available", e);
        }
    }
    
    public static enum NcdReportProcessedAuthorState {
        processed, ignored
    }
}
