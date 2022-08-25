
package com.fortify.cli.sc_dast.util;

// TODO Instead of depending on indexes, each enum entry should explicitly define the scan status type integer 
// TODO What would be the most appropriate package for this enum?
public enum SCDastScanStatusTypes {
    Queued, Pending, Paused, Running, Complete, Interrupted, Unknown,
    ImportingScanResults, ImportScanResultsFailed, FailedToStart, PausingScan,
    ResumingScan, CompletingScan, ResumeScanQueued, ForcedComplete, FailedToResume, LicenseUnavailable;

    public static String getStatusString(Integer index){
        return SCDastScanStatusTypes.values()[index-1].toString();
    }

}
