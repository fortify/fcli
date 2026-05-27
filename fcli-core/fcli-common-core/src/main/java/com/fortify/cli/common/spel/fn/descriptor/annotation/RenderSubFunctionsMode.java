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
package com.fortify.cli.common.spel.fn.descriptor.annotation;

/**
 * Controls how functions returned by a SpEL function should be rendered 
 * in documentation and schema.
 */
public enum RenderSubFunctionsMode {
    /**
     * Auto-detect: render as sections if the return type is annotated with @SpelFunctions,
     * otherwise render inline.
     */
    AUTO,
    
    /**
     * Render returned functions as separate sections with their own headers.
     */
    SECTION,
    
    /**
     * Render returned functions inline without separate section headers.
     */
    INLINE
}
