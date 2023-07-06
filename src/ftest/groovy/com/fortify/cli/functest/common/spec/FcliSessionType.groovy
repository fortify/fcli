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
        
        @Override public boolean isEnabled() { hasProperty(defaultPropertyName()) }

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
        
        String defaultPropertyName() {
            "ftest."+module()
        }
        boolean hasProperty(String propertyName) {
            StringUtils.isNotBlank(System.properties[propertyName])
        }
        String property(String propertyName) {
            System.properties[propertyName]
        }
        URI uri(String propertyName) {
            new URI(property(propertyName))
        }
        String baseUrl(URI uri) {
            return new URI(uri.scheme, uri.authority, uri.path, uri.query, uri.fragment)
        }
        String[] userInfo(URI uri) {
            def userInfo = uri?.userInfo
            def idx = userInfo?.lastIndexOf(':')
            if ( idx ) {
                return [userInfo.substring(0, idx), userInfo.substring(idx+1)]
            } 
        }
    }
    
    private static class SSCSessionHandler extends AbstractSessionHandler {
        @Override public String friendlyName() { "SSC" }
        @Override public String module() { "ssc" }

        @Override
        public String[] loginOptions() {
            def uri = uri(defaultPropertyName())
            return urlOptions(uri)+credentialOptions(uri)
        }

        @Override
        public String[] logoutOptions() {
            def uri = uri(defaultPropertyName())
            return credentialOptions(uri)
        }
        
        private String[] urlOptions(URI uri) {
            return ["--url", baseUrl(uri)]
        }
        
        private String[] credentialOptions(URI uri) {
            def userInfo = userInfo(uri)
            return [
                "--user", userInfo[0],
                "--password", userInfo[1],
            ]
        }
    } 
    
    private static class FoDSessionHandler extends AbstractSessionHandler {
        @Override public String friendlyName() { "FoD" }
        @Override public String module() { "fod" }
        
        @Override
        public String[] loginOptions() {
            def uri = uri(defaultPropertyName())
            return urlOptions(uri)+credentialOptions(uri)
        }

        @Override
        public String[] logoutOptions() {
            return []
        }
        
        private String[] urlOptions(URI uri) {
            return ["--url", baseUrl(uri)]
        }
        
        private String[] credentialOptions(URI uri) {
            def userInfo = userInfo(uri)
            if ( userInfo[0].contains(':') ) {
                def user = userInfo[0].split(':')
                return [
                    "--tenant", user[0],
                    "--user", user[1],
                    "--password", userInfo[1]
                ]
            } else {
              return [
                "--client-id", userInfo[0],
                "--client-secret", userInfo[1],
              ]
            }
        }
    }

    private static class SCSastSessionHandler extends AbstractSessionHandler {
        @Override public String friendlyName() { "ScanCentral SAST" }
        @Override public boolean isEnabled() { false }
        @Override public String module() { "sc-sast" }
        
        @Override
        public String[] loginOptions() {
            []
        }

        @Override
        public String[] logoutOptions() {
            []
        }
        
    }
    
    private static class SCDastSessionHandler extends AbstractSessionHandler {
        @Override public String friendlyName() { "ScanCentral DAST" }
        @Override public boolean isEnabled() { false }
        @Override public String module() { "sc-dast" }
        
        @Override
        public String[] loginOptions() {
            []
        }

        @Override
        public String[] logoutOptions() {
            []
        }
    }

}