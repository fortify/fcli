package com.fortify.cli.common.rest.wait;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitStatus;

public interface IWaitHelperProgressMonitor {
    void updateProgress(Map<ObjectNode, WaitStatus> recordsWithWaitStatus);
    void finish(Map<ObjectNode, WaitStatus> recordsWithWaitStatus);
}
