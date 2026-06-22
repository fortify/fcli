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
package com.fortify.cli.common.cli.util;

/**
 * Enum values representing the top-level fcli product modules. Used with the
 * {@link RequiresSessionFrom} annotation. Import individual constants statically
 * for concise usage, e.g.:
 * <pre>
 *   import static com.fortify.cli.common.cli.util.FcliModules.SSC;
 *   &#64;RequiresSessionFrom(SSC)
 * </pre>
 */
public enum FcliModules {
    ACTION, AGENT, AVIATOR, CONFIG, FOD, LICENSE, SC_DAST, SC_SAST, SSC, TOOL, UTIL;

    @Override
    public String toString() { return name().toLowerCase().replace('_', '-'); }
}
