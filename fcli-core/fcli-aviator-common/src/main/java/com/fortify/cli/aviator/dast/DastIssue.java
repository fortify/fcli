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
package com.fortify.cli.aviator.dast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;

/**
 * Represents a DAST issue from WebInspect scan results.
 */
@Data
public class DastIssue {
    private String id;
    private String checkTypeId;
    private String engineType;
    private String vulnerabilityId;
    private int severity;
    private String name;
    private String category;        // From 7PK Category classification
    private String cweId;           // From CWE classification
    private String cweDescription;  // Full CWE description text
    private String sessionUrl;      // URL of the session containing this issue
    private List<String> reproStepUrls = new ArrayList<>();
    private List<DastReproStep> reproSteps = new ArrayList<>();

    // ReportSections for audit context
    private String summary;         // Summary from ReportSection
    private String implication;     // Implication from ReportSection
    private String execution;       // Execution from ReportSection
    private String fix;             // Fix recommendation from ReportSection
    private String referenceInfo;   // Reference Info from ReportSection

    // Additional classifications
    private Map<String, String> classifications = new LinkedHashMap<>();  // kind -> value

    // Audit status
    private boolean suppressed = false;

    /**
     * SAST instance IDs that are already correlated to this DAST issue, as read
     * from {@code <ExternalFindings>/<ExternalFinding>/<OriginFindingID>} in the
     * webinspect.xml from a previous correlation run.
     */
    private Set<String> existingCorrelatedSastIds = new HashSet<>();
}
