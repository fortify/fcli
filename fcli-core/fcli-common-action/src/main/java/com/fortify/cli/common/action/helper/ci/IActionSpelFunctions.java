/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.action.helper.ci;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Common interface for all CI system SpEL function implementations.
 * Provides basic identification and environment data access.
 * 
 * @author rsenden
 */
public interface IActionSpelFunctions {
    /**
     * Get environment data as ObjectNode for use in actions.
     * Returns null if not running in the corresponding CI system.
     * 
     * @return Environment data or null if not in this CI system
     */
    ObjectNode getEnv();
    
    /**
     * Get the CI system type identifier.
     * 
     * @return CI system type: "github", "gitlab", "ado", or "unknown"
     */
    String getType();
}
