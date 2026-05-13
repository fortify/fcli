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

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public final class AviatorSSCTagDefs {
    @Getter
    @RequiredArgsConstructor
    public static final class TagDefinition {
        private final String guid;
        private final String name;
        /** SSC valueType: {@code "LIST"} for enum-style tags, {@code "TEXT"} for free-text tags. */
        private final String valueType;
        private final List<String> values;
    }

    public static final TagDefinition AVIATOR_PREDICTION_TAG = new TagDefinition(
            "C2D6EC66-CCB3-4FB9-9EE0-0BB02F51008F",
            "Aviator prediction",
            "LIST",
            List.of(
                    "AVIATOR:Not an Issue", "AVIATOR:Remediation Required", "AVIATOR:Unsure",
                    "AVIATOR:Excluded due to Limiting", "AVIATOR:Suspicious", "AVIATOR:Proposed Not an Issue"
            )
    );

    public static final TagDefinition AVIATOR_STATUS_TAG = new TagDefinition(
            "FB7B0462-2C2E-46D9-811A-DCC1F3C83051",
            "Aviator status",
            "LIST",
            List.of("PROCESSED_BY_AVIATOR", "PROCESSED_BY_AVIATOR_WITH_REMEDIATION")
    );

    /**
     * Free-text tag persisting SAST–DAST correlation status per issue.
     * Value format: {@code CORRELATED::DAST-1,DAST-2|REJECTED::DAST-3}
     * Written by {@code SastFprCorrelationRecorder} into the SAST FPR's audit.xml.
     * The GUID must match {@code SastFprCorrelationRecorder.DAST_CORRELATION_TAG_ID}.
     */
    public static final TagDefinition DAST_CORRELATION_STATUS_TAG = new TagDefinition(
            "7A3B5C9D-1E2F-4A8B-9C0D-E1F2A3B4C5D6",
            "DAST correlation status",
            "TEXT",
            List.of()
    );
}
