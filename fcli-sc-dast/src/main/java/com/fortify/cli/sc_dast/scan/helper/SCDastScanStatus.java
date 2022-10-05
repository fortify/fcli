
package com.fortify.cli.sc_dast.scan.helper;

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

}
