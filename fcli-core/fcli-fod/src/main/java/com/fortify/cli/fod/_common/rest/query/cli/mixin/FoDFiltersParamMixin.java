/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod._common.rest.query.cli.mixin;

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
