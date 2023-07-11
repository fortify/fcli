package com.fortify.cli.functest.common.spec;

import org.junit.platform.commons.util.StringUtils
import org.spockframework.runtime.extension.IMethodInterceptor

import com.fortify.cli.functest.common.runner.FcliRunner

public enum FcliSessionType {
    SSC(new SSCSessionHandler()),
    FOD(new FoDSessionHandler()),
    SCSAST(new SCSastSessionHandler()),
    SCDAST(new SCDastSessionHandler())
    
    final ISessionHandler handler
    
    private FcliSessionType(ISessionHandler handler) {
        this.handler = handler
    }
    
    static void logoutAll() {
        FcliSessionType.values().each { it.handler.logout() }
    }
    
    private interface ISessionHandler {
        String friendlyName();
        boolean isEnabled();
        IMethodInterceptor interceptor();
        void login();
        void logout();
    }   
    
    private static abstract class AbstractSessionHandler implements ISessionHandler {
        String[] STD_LOGIN_ARGS = [module(), "session","login"] 
        String[] STD_LOGOUT_ARGS = [module(), "session","logout"]
        private boolean loggedIn = false;
        private boolean failed = false;
        public IMethodInterceptor interceptor() {
            return {invocation->
                if ( isEnabled() ) {login(); invocation.proceed()}
            }
        }

        @Override
        public synchronized final void login() {
            if ( failed ) {
                throw new IllegalStateException("Skipped due to earlier "+friendlyName()+" login failure")
            } else if ( !loggedIn ) {
                println("Logging in to "+friendlyName())
                if ( !FcliRunner.run(STD_LOGIN_ARGS+loginOptions()) ) {
                     failed = true
                     throw new IllegalStateException("Error logging in to "+friendlyName())
                }
                loggedIn = true
            }
        }

        @Override
        public synchronized final void logout() {
            if ( loggedIn ) {
                FcliRunner.run(STD_LOGOUT_ARGS+logoutOptions())
                loggedIn = false
            }
        }
        
        abstract String module()
        abstract String[] loginOptions()
        abstract String[] logoutOptions()
        
        String basePropertyName() {
            "ftest."+module()
        }
        String get(String subPropertyName) {
            System.properties[basePropertyName()+"."+subPropertyName]
        }
        boolean has(String subPropertyName) {
            get(subPropertyName)
        }
        String[] option(String optName) {
            has(optName) ? ["--"+optName, get(optName)] : []
        }
        String[] options(String ... optNames) {
            return optNames.every { has(it) } 
                ? optNames.stream().map(this.&option).collect().flatten()
                : [] 
        } 
    }
    
    private static class SSCSessionHandler extends AbstractSessionHandler {
        @Override public String friendlyName() { "SSC" }
        @Override public String module() { "ssc" }
        
        @Override
        public boolean isEnabled() {
            has("url")
        }

        @Override
        public String[] loginOptions() {
            option("url")+options("user", "password")+options("token")+options("ci-token")
        }

        @Override
        public String[] logoutOptions() {
            def result = options("user", "password")
            return result.length==0 ? "--no-revoke-token" : result
        }
    } 
    
    private static class FoDSessionHandler extends AbstractSessionHandler {
        @Override public String friendlyName() { "FoD" }
        @Override public String module() { "fod" }
        
        @Override
        public boolean isEnabled() {
            has("url")
        }
        
        @Override
        public String[] loginOptions() {
            option("url")+options("tenant", "user", "password")+options("client-id", "client-secret")
        }

        @Override
        public String[] logoutOptions() {
            return []
        }
    }

    private static class SCSastSessionHandler extends AbstractSessionHandler {
        @Override public String friendlyName() { "ScanCentral SAST" }
        @Override public String module() { "sc-sast" }
        
        @Override
        public boolean isEnabled() {
            has("ssc-url")
        }

        @Override
        public String[] loginOptions() {
            option("ssc-url")+options("ssc-user", "ssc-password", "client-auth-token")+options("ssc-ci-token", "client-auth-token")
        }

        @Override
        public String[] logoutOptions() {
            def result = options("ssc-user", "ssc-password")
            return result.length==0 ? "--no-revoke-token" : result
        }
    }
    
    private static class SCDastSessionHandler extends AbstractSessionHandler {
        @Override public String friendlyName() { "ScanCentral DAST" }
        @Override public String module() { "sc-dast" }
        
        @Override
        public boolean isEnabled() {
            has("ssc-url")
        }

        @Override
        public String[] loginOptions() {
            options("ssc-url")+options("ssc-user", "ssc-password")+options("ssc-ci-token")
        }

        @Override
        public String[] logoutOptions() {
            def result = options("ssc-user", "ssc-password")
            return result.length==0 ? "--no-revoke-token" : result
        }
    }

}