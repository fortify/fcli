package com.fortify.cli.ssc.rest.query.cli.mixin;

import com.fortify.cli.common.rest.query.cli.mixin.AbstractServerSideQueryMixin;

import picocli.CommandLine.Option;

public class SSCQParamMixin extends AbstractServerSideQueryMixin {
    @Option(names="--q-param", required=false, descriptionKey="fcli.ssc.q-param")
    private String qParam;
    
    @Override
    protected String getServerSideQueryParamName() {
        return "q";
    }
    
    @Override
    protected String getServerSideQueryParamOptionValue() {
        return qParam;
    }
}
