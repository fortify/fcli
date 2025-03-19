package com.fortify.cli.aviator.config;

public interface IAviatorLogger {
    void progress(String format, Object... args);
    void writeInfo(String message, Object... args);
    void info(String format, Object... args);
    void warn(String format, Object... args);
    void error(String format, Object... args);
}