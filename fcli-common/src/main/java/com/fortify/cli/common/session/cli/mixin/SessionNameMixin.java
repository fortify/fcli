/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.session.cli.mixin;

import com.fortify.cli.common.util.StringUtils;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SessionNameMixin {
    private static abstract class AbstractSessionNameMixin {
        protected abstract String getSessionNameOrNull();
        public final String getSessionName() {
            return hasSessionName() ? getSessionNameOrNull() : "default";
        }
        
        public final boolean hasSessionName() {
            return StringUtils.isNotBlank(getSessionNameOrNull());
        }
    }
    
    public static class OptionalOption extends AbstractSessionNameMixin {
        @ArgGroup(headingKey = "arggroup.optional.session-name.heading", order = 20)
        private SessionNameArgGroup nameOptions = new SessionNameArgGroup();
    
        static class SessionNameArgGroup {
            @Option(names = {"--session"}, required = false)
            private String sessionName;
        }
        @Override
        protected String getSessionNameOrNull() {
            return nameOptions.sessionName;
        }
    }
    
    public static class OptionalParameter extends AbstractSessionNameMixin {
        @ArgGroup(headingKey = "arggroup.optional.session-name.heading", order = 1000)
        private SessionNameArgGroup nameOptions = new SessionNameArgGroup();
    
        static class SessionNameArgGroup {
            @Parameters(arity="0..1", index="0", paramLabel="<session>")
            private String sessionName;
        }
        @Override
        protected String getSessionNameOrNull() {
            return nameOptions.sessionName;
        }
    }
}
