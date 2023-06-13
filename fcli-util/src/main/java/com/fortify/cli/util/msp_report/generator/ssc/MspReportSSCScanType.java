package com.fortify.cli.util.msp_report.generator.ssc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MspReportSSCScanType {
    SAST(true), DAST(true), RUNTIME(true), OTHER(false);
    
    @Getter private final boolean fortifyScan;
}