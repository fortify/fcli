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
package com.fortify.cli.app;

import com.fortify.cli.app.runner.DefaultFortifyCLIRunner;
import com.fortify.cli.common.util.ConsoleHelper;

/**
 * <p>This class provides the {@link #main(String[])} entrypoint into the application,
 * and also registers some GraalVM features, allowing the application to run properly 
 * as GraalVM native images.</p>
 * 
 * @author Ruud Senden
 */
public class FortifyCLI {
    /**
     * This is the main entry point for executing the Fortify CLI.
     * @param args Command line options passed to Fortify CLI
     */
    public static final void main(String[] args) {
        System.exit(execute(args));
    }

    private static final int execute(String[] args) {
        try {
            ConsoleHelper.installAnsiConsole();
            return DefaultFortifyCLIRunner.run(args);
        } finally {
            ConsoleHelper.uninstallAnsiConsole();
        }
    }
}
