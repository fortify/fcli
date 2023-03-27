package com.fortify.cli.scm.github.helper;

import picocli.CommandLine.ITypeConverter;

public final class GitHubRepoSlugTypeConverter implements ITypeConverter<GitHubRepoSlugDescriptor> {
    @Override
    public GitHubRepoSlugDescriptor convert(String value) throws Exception {
        String[] split = value.split("/");
        if ( split.length!=2 ) {
            throw new IllegalArgumentException("Repositories must be specified as <organization|account>/<repo>, current value: "+value);
        }
        return new GitHubRepoSlugDescriptor(split[0], split[1]);
    }  
}