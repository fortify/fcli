/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.session.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SessionNameMixin {
    public static class OptionalOption {
        @ArgGroup(headingKey = "arggroup.optional.session-name.heading", order = 20)
        private SessionNameArgGroup nameOptions = new SessionNameArgGroup();
    
        static class SessionNameArgGroup {
            @Option(names = {"--session"}, required = true, defaultValue="default")
            private String sessionName;
        }
        protected String getSessionName() {
            return nameOptions.sessionName;
        }
    }
    
    public static class OptionalLoginOption {
        @Option(names = {"--session"}, required = true, defaultValue="default", descriptionKey = "login.session")
        @Getter private String sessionName;
    }
    
    public static class OptionalLogoutOption {
        @Option(names = {"--session"}, required = true, defaultValue="default", descriptionKey = "logout.session")
        @Getter private String sessionName;
    }
}
