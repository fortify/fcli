/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/

package com.fortify.cli.ssc.report.helper;

import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Enum values copied from SSC internal enum (com.fortify.manager.DAO.report.ReportStatus in ssc-core-[version].jar)
 */
public enum SSCReportStatus {
    SCHED_PROCESSING, 
    PROCESSING, 
    PROCESS_COMPLETE, 
    ERROR_PROCESSING;
    
    public static final SSCReportStatus[] getFailureStates() {
        return new SSCReportStatus[]{
            ERROR_PROCESSING
        };
    }
    
    public static final SSCReportStatus[] getKnownStates() {
        return SSCReportStatus.values();
    }
    
    public static final SSCReportStatus[] getDefaultCompleteStates() {
        return new SSCReportStatus[]{ PROCESS_COMPLETE };
    }
    
    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(SSCReportStatus::name).toArray(String[]::new);
    }
    
    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(SSCReportStatus::name).toArray(String[]::new);
    }
    
    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(SSCReportStatus::name).toArray(String[]::new);
    }
    
    public static final class SSCReportStatusIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public SSCReportStatusIterable() { 
            super(Stream.of(SSCReportStatus.values()).map(SSCReportStatus::name).toList()); 
        }
    }

}
