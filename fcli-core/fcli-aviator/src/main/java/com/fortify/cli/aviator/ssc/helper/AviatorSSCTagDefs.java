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
        private final List<String> values;
    }

    public static final TagDefinition AVIATOR_PREDICTION_TAG = new TagDefinition(
            "C2D6EC66-CCB3-4FB9-9EE0-0BB02F51008F",
            "Aviator prediction",
            List.of(
                    "AVIATOR:Not an Issue", "AVIATOR:Remediation Required", "AVIATOR:Unsure",
                    "AVIATOR:Excluded due to Limiting", "AVIATOR:Suspicious", "AVIATOR:Proposed Not an Issue"
            )
    );

    public static final TagDefinition AVIATOR_STATUS_TAG = new TagDefinition(
            "FB7B0462-2C2E-46D9-811A-DCC1F3C83051",
            "Aviator status",
            List.of("PROCESSED_BY_AVIATOR")
    );
}