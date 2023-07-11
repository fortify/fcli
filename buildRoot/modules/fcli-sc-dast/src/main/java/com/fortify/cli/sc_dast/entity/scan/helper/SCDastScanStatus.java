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

package com.fortify.cli.sc_dast.entity.scan.helper;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// TODO Instead of depending on indexes, each enum entry should explicitly define the scan status type integer 
// TODO What would be the most appropriate package for this enum?
public enum SCDastScanStatus {
    Queued, Pending, Paused, Running, Complete, Interrupted, Unknown,
    ImportingScanResults, ImportScanResultsFailed, FailedToStart, PausingScan,
    ResumingScan, CompletingScan, ResumeScanQueued, ForcedComplete, FailedToResume, LicenseUnavailable;

    public int getScanStatusType() {
        return ordinal()+1;
    }
    
    public static SCDastScanStatus valueOf(Integer index){
        return SCDastScanStatus.values()[index-1];
    }
    
    public static JsonNode addScanStatus(JsonNode scanRecord) {
        ObjectNode record = scanRecord==null || !(scanRecord instanceof ObjectNode) 
                ? null 
                : (ObjectNode)scanRecord;
        if ( record != null ) {
            int scanStatusType = record.get("scanStatusType").asInt();
            return record.put("scanStatus", SCDastScanStatus.valueOf(scanStatusType).toString());
        }
        return scanRecord;
    }
    
    public static final SCDastScanStatus[] getFailureStates() {
        return new SCDastScanStatus[]{
            Interrupted, Unknown, ImportScanResultsFailed, FailedToStart, FailedToResume, LicenseUnavailable
        };
    }
    
    public static final SCDastScanStatus[] getKnownStates() {
        return SCDastScanStatus.values();
    }
    
    public static final SCDastScanStatus[] getDefaultCompleteStates() {
        return new SCDastScanStatus[]{ Complete };
    }
    
    public static final String[] getFailureStateNames() {
        return Stream.of(getFailureStates()).map(SCDastScanStatus::name).toArray(String[]::new);
    }
    
    public static final String[] getKnownStateNames() {
        return Stream.of(getKnownStates()).map(SCDastScanStatus::name).toArray(String[]::new);
    }
    
    public static final String[] getDefaultCompleteStateNames() {
        return Stream.of(getDefaultCompleteStates()).map(SCDastScanStatus::name).toArray(String[]::new);
    }

}
