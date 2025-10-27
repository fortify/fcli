/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.sc_sast.scan.helper;

import java.util.ArrayList;
import java.util.stream.Stream;

import com.fortify.cli.ssc.artifact.helper.SSCArtifactStatus;

/**
 * This is a copy of {@link SSCArtifactStatus}, but adds states from 
 * {@link SCSastScanJobState} so we can copy scanState/publishState
 * to sscArtifactState where needed.
 */
public enum SCSastScanJobArtifactState {
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
    TIMEOUT,
    
    // Publish disabled state
    NO_PUBLISH
    ;
    
    public static final SCSastScanJobArtifactState[] getFailureStates() {
        return new SCSastScanJobArtifactState[]{
            FAILED, FAULTED, TIMEOUT, CANCELING, CANCELED, REQUIRE_AUTH, ERROR_PROCESSING, AUTH_DENIED, ERROR_DELETING, ERROR_PURGING, ERROR_DISPATCH, ERROR_ANALYZING, UNKNOWN, AUDIT_FAILED
        };
    }
    
    public static final SCSastScanJobArtifactState[] getKnownStates() {
        return SCSastScanJobArtifactState.values();
    }
    
    public static final SCSastScanJobArtifactState[] getDefaultCompleteStates() {
        return new SCSastScanJobArtifactState[]{ PROCESS_COMPLETE };
    }
    
    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(SCSastScanJobArtifactState::name).toArray(String[]::new);
    }
    
    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(SCSastScanJobArtifactState::name).toArray(String[]::new);
    }
    
    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(SCSastScanJobArtifactState::name).toArray(String[]::new);
    }
    
    public static final class SCSastControllerScanJobArtifactStateIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public SCSastControllerScanJobArtifactStateIterable() { 
            super(Stream.of(SCSastScanJobArtifactState.values()).map(Enum::name).toList()); 
        }
    }

}
