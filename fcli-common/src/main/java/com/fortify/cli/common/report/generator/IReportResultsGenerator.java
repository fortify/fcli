package com.fortify.cli.common.report.generator;

public interface IReportResultsGenerator extends Runnable, AutoCloseable {
    // Override AutoCloseable::close to not throw any checked exceptions
    @Override void close();
}
