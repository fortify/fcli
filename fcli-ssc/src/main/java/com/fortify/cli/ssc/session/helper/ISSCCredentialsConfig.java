package com.fortify.cli.ssc.session.helper;

public interface ISSCCredentialsConfig {
    char[] getPredefinedToken();
    ISSCUserCredentialsConfig getUserCredentialsConfig();
}
