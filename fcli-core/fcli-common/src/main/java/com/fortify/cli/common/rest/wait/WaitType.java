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
package com.fortify.cli.common.rest.wait;

import java.util.Set;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public final class WaitType {
    private final LoopType loopType;
    private final AnyOrAll anyOrAll;
    
    public static enum AnyOrAll {
        any_match, all_match
    }
    
    @RequiredArgsConstructor
    public static enum LoopType {
        Until(false),
        While(true);
        
        private final boolean waitIfMatching;
        
        public boolean isWaiting(Set<String> statesToMatch, String currentState) {
            return waitIfMatching == matches(statesToMatch, currentState);
        }
        
        private static boolean matches(Set<String> statesToMatch, String currentState) {
            return statesToMatch.contains(currentState);
        }
    }
}