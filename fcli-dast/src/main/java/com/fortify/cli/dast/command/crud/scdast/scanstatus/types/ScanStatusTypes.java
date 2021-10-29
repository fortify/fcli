
package com.fortify.cli.dast.command.crud.scdast.scanstatus.types;

public enum ScanStatusTypes {
	Queued, Pending, Paused, Running, Complete, Interrupted, Unknown,
	ImportingScanResults, ImportScanResultsFailed, FailedToStart, PausingScan,
	ResumingScan, CompletingScan, ResumeScanQueued, ForcedComplete, FailedToResume, LicenseUnavailable;

	public static String getStatusString(Integer index){
		return ScanStatusTypes.values()[index].toString();
	}

}
