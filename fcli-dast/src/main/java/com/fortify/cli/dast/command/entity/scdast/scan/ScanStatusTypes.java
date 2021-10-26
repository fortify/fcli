
package com.fortify.cli.dast.command.entity.scdast.scan;

import com.fortify.cli.common.output.writer.*;
import lombok.Getter;

public enum ScanStatusTypes {
	Queued, Pending, Paused, Running, Complete, Interrupted, Unknown,
	ImportingScanResults, ImportScanResultsFailed, FailedToStart, PausingScan,
	ResumingScan, CompletingScan, ResumeScanQueued, ForcedComplete, FailedToResume, LicenseUnavailable
}
