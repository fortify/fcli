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

package com.fortify.cli.ssc.artifact.helper;

import java.util.stream.Stream;

/**
 * Enum values copied from SSC internal enum (com.fortify.manager.DAO.artifact.ArtifactStatus in ssc-core-[version].jar)
 */
public enum SSCArtifactStatus {
    SCHED_PROCESSING, 
    PROCESSING, 
    PROCESS_COMPLETE, 
    ERROR_PROCESSING, 
    AUTH_DENIED, 
    REQUIRE_AUTH, 
    DELETING, 
    ERROR_DELETING, 
    DELETED, 
    PURGING, 
    PURGED, 
    ERROR_PURGING, 
    DISPATCH_ANALYSIS, 
    DISPATCH_REAUDIT, 
    ERROR_DISPATCH, 
    QUEUED_ANALYSIS, 
    REQUEUED_ANALYSIS, 
    ANALYZING, 
    ANALYSIS_COMPLETE, 
    ERROR_ANALYZING, 
    UNKNOWN, 
    AUDIT_FAILED;
    
    public static final SSCArtifactStatus[] getFailureStates() {
        return new SSCArtifactStatus[]{
            REQUIRE_AUTH, ERROR_PROCESSING, AUTH_DENIED, ERROR_DELETING, ERROR_PURGING, ERROR_DISPATCH, ERROR_ANALYZING, UNKNOWN, AUDIT_FAILED
        };
    }
    
    public static final SSCArtifactStatus[] getKnownStates() {
        return SSCArtifactStatus.values();
    }
    
    public static final SSCArtifactStatus[] getDefaultCompleteStates() {
        return new SSCArtifactStatus[]{ PROCESS_COMPLETE };
    }
    
    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(SSCArtifactStatus::name).toArray(String[]::new);
    }
    
    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(SSCArtifactStatus::name).toArray(String[]::new);
    }
    
    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(SSCArtifactStatus::name).toArray(String[]::new);
    }

}
