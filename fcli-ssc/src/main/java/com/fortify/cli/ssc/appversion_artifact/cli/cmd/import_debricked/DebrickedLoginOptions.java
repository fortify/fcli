package com.fortify.cli.ssc.appversion_artifact.cli.cmd.import_debricked;

import com.fortify.cli.common.rest.runner.config.IUserCredentialsConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

@ReflectiveAccess
public class DebrickedLoginOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private DebrickedUrlConfigOptions urlConfigOptions = new DebrickedUrlConfigOptions();
    
    @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
    @Getter private DebrickedAuthOptions authOptions = new DebrickedAuthOptions();
    
    @ReflectiveAccess
    public static class DebrickedAuthOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private DebrickedUserCredentialOptions userCredentialsOptions;
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private DebrickedAccessTokenCredentialOptions tokenOptions;
    }
    
    @ReflectiveAccess
    public static class DebrickedUserCredentialOptions implements IUserCredentialsConfig {
    	@Option(names = {"--debricked-user", "-u"}, required = true)
        @Getter private String user;
        
        @Option(names = {"--debricked-password", "-p"}, interactive = true, echo = false, arity = "0..1", required = true)
        @Getter private char[] password;
    }
    
    @ReflectiveAccess
    public static class DebrickedAccessTokenCredentialOptions {
        @Option(names = {"--debricked-access-token", "-t"}, interactive = true, echo = false, arity = "0..1", required = true)
        @Getter private char[] accessToken;
    }
}
