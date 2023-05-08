package com.fortify.cli.common.report.config;

import java.util.Collection;

import com.fortify.cli.common.report.collector.IReportResultsCollector;

public interface IReportSourceSupplierConfig<R extends IReportResultsCollector> {
    Collection<? extends IReportSourceConfig<R>> getSourceConfigs();
}
