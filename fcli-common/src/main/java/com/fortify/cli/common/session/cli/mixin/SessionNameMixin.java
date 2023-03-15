/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.common.session.cli.mixin;

import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SessionNameMixin {
    @ReflectiveAccess
    private static abstract class AbstractSessionNameMixin {
        protected abstract String getSessionNameOrNull();
        public final String getSessionName() {
            return hasSessionName() ? getSessionNameOrNull() : "default";
        }
        
        public final boolean hasSessionName() {
            return StringUtils.isNotBlank(getSessionNameOrNull());
        }
    }
    
    @ReflectiveAccess
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
    
    @ReflectiveAccess
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
