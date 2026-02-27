/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.ftest._common.spec

import com.fortify.cli.ftest._common.Fcli

import spock.lang.Specification

/**
 * Base specification class for all fcli functional tests.
 * Provides common utility methods for test setup and cleanup.
 *
 * @author Ruud Senden
 */
class FcliBaseSpec extends Specification {
    /**
     * Cleanup all tool installations to prevent state leakage between test suites.
     * This method attempts to uninstall all versions of all known tools, ignoring
     * any errors (e.g., if a tool is not installed).
     */
    protected static void cleanupAllTools() {
        def tools = ['sc-client', 'fcli', 'debricked-cli', 'fod-uploader', 
                     'bugtracker-utility', 'vuln-exporter']
        tools.each { tool ->
            try {
                Fcli.run("tool ${tool} uninstall -y -v=all --progress none", {})
            } catch (Exception e) {
                // Ignore errors - tool may not be installed
            }
        }
    }
}
