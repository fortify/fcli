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
package com.fortify.cli.common.util;

/**
 * This enum is generally used in loops and other types of processing 
 * to indicate whether processing should continue ({@link #FALSE} or
 * should be stopped ({@link #TRUE}. For code readability, this is
 * much more clear than a boolean, as potentially some code could
 * interpret {@link Boolean#TRUE} as 'continue', whereas other code
 * could interpret {@link Boolean#TRUE} as 'break'.
 */
public enum Break {
    FALSE, TRUE;
    
    public boolean doBreak() {
        return this==Break.TRUE;
    }
    
    public boolean doContinue() {
        return this==Break.FALSE;
    }
}