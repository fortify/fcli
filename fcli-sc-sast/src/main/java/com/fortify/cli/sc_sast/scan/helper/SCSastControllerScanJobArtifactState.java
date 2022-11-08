
package com.fortify.cli.sc_sast.scan.helper;

import java.util.stream.Stream;

import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactStatus;

/**
 * This is a copy of {@link SSCAppVersionArtifactStatus}, but adds states from 
 * {@link SCSastControllerScanJobState} so we can copy scanState/sscUploadState
 * to sscArtifactState where needed.
 */
public enum SCSastControllerScanJobArtifactState {
    // SSC artifact states
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
    AUDIT_FAILED,
    
    // ScanCentral SAST job states
    PENDING, 
    QUEUED, 
    RUNNING, 
    CANCELING, 
    CANCELED, 
    COMPLETED, 
    FAILED, 
    FAULTED, 
    TIMEOUT;
    ;
    
    public static final SCSastControllerScanJobArtifactState[] getFailureStates() {
        return new SCSastControllerScanJobArtifactState[]{
            FAILED, FAULTED, TIMEOUT, CANCELING, CANCELED, REQUIRE_AUTH, ERROR_PROCESSING, AUTH_DENIED, ERROR_DELETING, ERROR_PURGING, ERROR_DISPATCH, ERROR_ANALYZING, UNKNOWN, AUDIT_FAILED
        };
    }
    
    public static final SCSastControllerScanJobArtifactState[] getKnownStates() {
        return SCSastControllerScanJobArtifactState.values();
    }
    
    public static final SCSastControllerScanJobArtifactState[] getDefaultCompleteStates() {
        return new SCSastControllerScanJobArtifactState[]{ PROCESS_COMPLETE };
    }
    
    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(SCSastControllerScanJobArtifactState::name).toArray(String[]::new);
    }
    
    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(SCSastControllerScanJobArtifactState::name).toArray(String[]::new);
    }
    
    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(SCSastControllerScanJobArtifactState::name).toArray(String[]::new);
    }

}
