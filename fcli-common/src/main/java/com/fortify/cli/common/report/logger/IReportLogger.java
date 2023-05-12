package com.fortify.cli.common.report.logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IReportLogger {
    void warn(String msg, Object... args);
    void warn(String msg, Exception e, Object... args);
    void error(String msg, Object... args);
    void error(String msg, Exception e, Object... args);
    void updateSummary(ObjectNode summary);
}