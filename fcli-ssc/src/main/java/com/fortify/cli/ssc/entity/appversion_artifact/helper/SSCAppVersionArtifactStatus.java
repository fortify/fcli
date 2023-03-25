
package com.fortify.cli.ssc.entity.appversion_artifact.helper;

import java.util.stream.Stream;

/**
 * Enum values copied from SSC internal enum (com.fortify.manager.DAO.artifact.ArtifactStatus in ssc-core-[version].jar)
 */
public enum SSCAppVersionArtifactStatus {
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
    
    public static final SSCAppVersionArtifactStatus[] getFailureStates() {
        return new SSCAppVersionArtifactStatus[]{
            REQUIRE_AUTH, ERROR_PROCESSING, AUTH_DENIED, ERROR_DELETING, ERROR_PURGING, ERROR_DISPATCH, ERROR_ANALYZING, UNKNOWN, AUDIT_FAILED
        };
    }
    
    public static final SSCAppVersionArtifactStatus[] getKnownStates() {
        return SSCAppVersionArtifactStatus.values();
    }
    
    public static final SSCAppVersionArtifactStatus[] getDefaultCompleteStates() {
        return new SSCAppVersionArtifactStatus[]{ PROCESS_COMPLETE };
    }
    
    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(SSCAppVersionArtifactStatus::name).toArray(String[]::new);
    }
    
    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(SSCAppVersionArtifactStatus::name).toArray(String[]::new);
    }
    
    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(SSCAppVersionArtifactStatus::name).toArray(String[]::new);
    }

}
