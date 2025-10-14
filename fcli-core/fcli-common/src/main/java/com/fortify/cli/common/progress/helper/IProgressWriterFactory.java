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
package com.fortify.cli.common.progress.helper;

/**
 * This interface provides methods for creating {@link IProgressWriterI18n} instances.
 * @author Ruud Senden
 */
public interface IProgressWriterFactory {
    /** Get the configured progress writer type */
    ProgressWriterType getType();
    /** Create a progress writer for the configured type */
    IProgressWriterI18n create();
    /** Create a progress writer for the given type, ignoring the configured type */
    IProgressWriterI18n create(ProgressWriterType progressWriterType);
}