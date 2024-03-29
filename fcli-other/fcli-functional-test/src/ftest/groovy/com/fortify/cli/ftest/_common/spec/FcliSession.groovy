package com.fortify.cli.ftest._common.spec;

import static java.lang.annotation.ElementType.METHOD
import static java.lang.annotation.ElementType.TYPE
import static java.lang.annotation.RetentionPolicy.RUNTIME

import java.lang.annotation.Retention
import java.lang.annotation.Target

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Input

@Target([METHOD, TYPE])
@Retention(RUNTIME)
@interface FcliSession {
    FcliSessionType[] value()
    
    public static enum FcliSessionType {
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
        
        public interface ISessionHandler {
            String friendlyName();
            boolean isEnabled();
            boolean login();
            void logout();
            List<String> getMaskedProperties();
        }
        
        private static abstract class AbstractSessionHandler implements ISessionHandler {
            def STD_LOGIN_ARGS = [module(), "session","login"]
            def STD_LOGOUT_ARGS = [module(), "session","logout"]
            private boolean loggedIn = false;
            private boolean failed = false;
    
            @Override
            public final boolean login() {
                if ( !loggedIn && !failed ) {
                    println("Logging in to "+friendlyName())
                    try {
                        def loginCredentialOptions = loginCredentialOptions()
                        if ( loginCredentialOptions==null || loginCredentialOptions.size()==0 ) {
                            throw new IllegalArgumentException("No or incomplete "+friendlyName()+" credentials provided, tests will be skipped")
                        }
                        def valuesToMask = values(maskedProperties)
                        Fcli.stringsToMask += valuesToMask
                        Fcli.run(
                            STD_LOGIN_ARGS+loginOptions()+loginCredentialOptions,
                            {it.expectSuccess(true, "Error logging in to "+friendlyName()+", tests will be skipped")}
                        )
                            
                        loggedIn = true
                    } catch ( Exception e ) {
                        e.printStackTrace()
                        failed = true
                    }
                }
                return !failed;
            }
    
            @Override
            public synchronized final void logout() {
                if ( loggedIn ) {
                    Fcli.stringsToMask += values(maskedProperties)
                    def result = Fcli.run(
                        STD_LOGOUT_ARGS+logoutOptions(),
                        {
                            if ( !it.success ) {
                                err.println("Error logging out from "+friendlyName()+"\n"+it.stderr.join("\n   "))
                            }
                        })
                    loggedIn = false
                }
            }
            
            abstract String module()
            abstract List<String> loginOptions()
            abstract List<String> loginCredentialOptions()
            abstract List<String> logoutOptions()
            
            String basePropertyName() {
                Input.addPropertyPrefix(module())
            }
            String get(String subPropertyName) {
                System.properties[basePropertyName()+"."+subPropertyName]
            }
            boolean has(String subPropertyName) {
                get(subPropertyName)
            }
            List<String> option(String optName) {
                has(optName) ? ["--"+optName, get(optName)] : []
            }
            List<String> options(String ... optNames) {
                return optNames.every { has(it) }
                    ? optNames.stream().map(this.&option).collect().flatten()
                    : []
            }
            List<String> values(List<String> optNames) {
                return optNames.stream().map(this.&get).filter({it!=null}).collect().flatten();
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
            public List<String> loginOptions() {
                option("url")
            }
            
            @Override
            public List<String> loginCredentialOptions() {
                options("user", "password")+options("token")+options("ci-token")
            }
    
            @Override
            public List<String> logoutOptions() {
                def result = options("user", "password")
                return result.size()==0 ? ["--no-revoke-token"] : result
            }
            
            @Override
            public List<String> getMaskedProperties() {
                ["url", "user", "password", "token", "ci-token"]
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
            public List<String> loginOptions() {
                option("url")
            }
            
            @Override
            public List<String> loginCredentialOptions() {
                options("tenant", "user", "password")+options("client-id", "client-secret")
            }
    
            @Override
            public List<String> logoutOptions() {
                return []
            }
            
            @Override
            public List<String> getMaskedProperties() {
                ["url", "tenant", "user", "password", "client-id", "client-secret"]
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
            public List<String> loginOptions() {
                option("ssc-url")
            }
            
            @Override
            public List<String> loginCredentialOptions() {
                options("ssc-user", "ssc-password", "client-auth-token")+options("ssc-ci-token", "client-auth-token")
            }
    
            @Override
            public List<String> logoutOptions() {
                def result = options("ssc-user", "ssc-password")
                return result.size()==0 ? "--no-revoke-token" : result
            }
            
            @Override
            public List<String> getMaskedProperties() {
                ["ssc-url", "ssc-user", "ssc-password", "ssc-ci-token", "client-auth-token"]
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
            public List<String> loginOptions() {
                options("ssc-url")
            }
            
            @Override
            public List<String> loginCredentialOptions() {
                options("ssc-user", "ssc-password")+options("ssc-ci-token")
            }
    
            @Override
            public List<String> logoutOptions() {
                def result = options("ssc-user", "ssc-password")
                return result.size()==0 ? "--no-revoke-token" : result
            }
            
            @Override
            public List<String> getMaskedProperties() {
                ["ssc-url", "ssc-user", "ssc-password", "ssc-ci-token"]
            }
        }
    
    }
}