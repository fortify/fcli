
package com.fortify.cli.ssc.appversion_artifact.helper;

import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Enum values and purgeable property copied from SSC internal enum. Purgeable is currently 
 * not used and probably better to let SSC decide whether purging is allowed in case this 
 * changes in future versions. Keeping it here allows for easy copying of updated enum values 
 * from the SSC implementation (com.fortify.manager.DAO.artifact.ArtifactStatus in ssc-core-[version].jar)
 */
public enum SSCAppVersionArtifactStatus {
    SCHED_PROCESSING(false), 
    PROCESSING(false), 
    PROCESS_COMPLETE, 
    ERROR_PROCESSING(false), 
    AUTH_DENIED(false), 
    REQUIRE_AUTH(false), 
    DELETING(false), 
    ERROR_DELETING, 
    DELETED(false), 
    PURGING(false), 
    PURGED(false), 
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
    
    @Getter(AccessLevel.PRIVATE) private final boolean purgeable;
    private SSCAppVersionArtifactStatus(final boolean purgeable) {
        this.purgeable = purgeable;
    }
    private SSCAppVersionArtifactStatus() {
        this.purgeable = true;
    }
    
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
