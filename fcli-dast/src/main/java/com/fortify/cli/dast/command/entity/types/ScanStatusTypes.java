
package com.fortify.cli.dast.command.entity.types;

// TODO Instead of depending on indexes, each enum entry should explicitly define the scan status type integer 
public enum ScanStatusTypes {
	Queued, Pending, Paused, Running, Complete, Interrupted, Unknown,
	ImportingScanResults, ImportScanResultsFailed, FailedToStart, PausingScan,
	ResumingScan, CompletingScan, ResumeScanQueued, ForcedComplete, FailedToResume, LicenseUnavailable;

	public static String getStatusString(Integer index){
		return ScanStatusTypes.values()[index-1].toString();
	}

}
