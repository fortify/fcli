package com.fortify.cli.ssc.token.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUrlConfigSupplier;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.ssc.session.manager.SSCSessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

@FixInjection
public class SSCTokenCommandUrlConfigMixin implements IUrlConfigSupplier {
    @Inject @ReflectiveAccess private SSCSessionDataManager sessionDataManager;
    
    @ArgGroup(exclusive=true)
    private SSCUrlConfigOrSessionName options = new SSCUrlConfigOrSessionName();
    
    private static final class SSCUrlConfigOrSessionName {
        @ArgGroup(exclusive=false) private UrlConfigOptions urlConfig = new UrlConfigOptions();
        @Option(names="--session") private String sessionName;
    } 
    
    @Override
    public IUrlConfig getUrlConfig() {
        if ( options!=null && options.urlConfig!=null && options.urlConfig.hasUrlConfig() ) {
            return options.urlConfig;
        } else if ( options!=null && options.sessionName!=null ) {
            return sessionDataManager.get(options.sessionName, true).getUrlConfig();
        } else {
            return sessionDataManager.get("default", true).getUrlConfig();
        }
    }
}
