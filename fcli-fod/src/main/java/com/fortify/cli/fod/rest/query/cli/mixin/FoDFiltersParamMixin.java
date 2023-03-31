package com.fortify.cli.fod.rest.query.cli.mixin;

import com.fortify.cli.common.rest.query.cli.mixin.AbstractServerSideQueryMixin;

import picocli.CommandLine.Option;

public class FoDFiltersParamMixin extends AbstractServerSideQueryMixin {
    @Option(names="--filters-param", required=false, descriptionKey="fcli.fod.filters-param")
    private String filtersParam;
    
    @Override
    protected String getServerSideQueryParamName() {
        return "filters";
    }
    
    @Override
    protected String getServerSideQueryParamOptionValue() {
        return filtersParam;
    }
}
