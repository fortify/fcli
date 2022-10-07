package com.fortify.cli.ssc.session.manager;

public interface ISSCCredentialsConfig {
    char[] getPredefinedToken();
    ISSCUserCredentialsConfig getUserCredentialsConfig();
}
