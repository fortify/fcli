/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Base type for all fcli-specific runtime exceptions surfaced to the CLI layer.
 * <p>Key responsibilities:
 * <ul>
 *   <li>Provides optional {@code exitCode} override; if {@code null}, picocli's default exit code is used.</li>
 *   <li>Defines a unified set of convenience constructors with {@code String.format}-style formatting.</li>
 *   <li>Declares {@link #getStackTraceString()} that subclasses implement to control user-facing output:
 *       <ul>
 *         <li>Simple exceptions return a concise summary without full stack trace unless underlying cause is technical.</li>
 *         <li>Technical/bug exceptions return full stack traces for diagnostics.</li>
 *       </ul>
 *   </li>
 * </ul>
 * Subclasses MUST implement {@link #getStackTraceString()} rather than manually concatenating stack trace text; the
 * {@code FcliExecutionExceptionHandler} relies on this for consistent formatting.
 */
public abstract class AbstractFcliException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    @Getter @Setter @Accessors(fluent=true) private Integer exitCode;

    public AbstractFcliException() {}
    
    public AbstractFcliException(String fmt, Object... args) {
        super(String.format(fmt, args));
    }

    public AbstractFcliException(String message) {
        super(message);
    }

    public AbstractFcliException(Throwable cause) {
        super(cause);
    }

    public AbstractFcliException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public abstract String getStackTraceString();
}
