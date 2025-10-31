/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.rest.query.cli.mixin;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.spel.query.QueryExpression;

import kong.unirest.HttpRequest;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command
public abstract class AbstractServerSideQueryMixin implements IHttpRequestUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractServerSideQueryMixin.class);
    @Getter @Mixin private CommandHelperMixin commandHelper;
    
    @Override
    public final HttpRequest<?> updateRequest(HttpRequest<?> request) {
        String serverSideQueryParamValue = getServerSideQueryParamOptionValue();
        String serverSidequeryParamName = getServerSideQueryParamName();
        if ( StringUtils.isBlank(serverSideQueryParamValue) ) {
            IServerSideQueryParamGeneratorSupplier generatorSupplier = 
                    getCommandHelper().getCommandAs(IServerSideQueryParamGeneratorSupplier.class)
                    .orElseThrow(()->new RuntimeException("Command must implement IQueryParamGeneratorSupplier: "+getCommandHelper().getCommand().getClass().getName()));
            Expression expression = getSpelExpression();
            if ( expression!=null  ) {
                serverSideQueryParamValue = generatorSupplier.getServerSideQueryParamGenerator()
                        .getServerSideQueryParamValue(expression);
            }
        }
        if ( StringUtils.isBlank(serverSideQueryParamValue) ) {
            LOG.debug("Not adding "+serverSidequeryParamName+" parameter");
            return request;
        } else {
            LOG.debug("Adding "+serverSidequeryParamName+" parameter with value: {}", serverSideQueryParamValue);
            return request.queryString(serverSidequeryParamName, serverSideQueryParamValue);
        }
    }
    
    protected abstract String getServerSideQueryParamName();
    protected abstract String getServerSideQueryParamOptionValue();

    protected final Expression getSpelExpression() {
        return FcliCommandSpecHelper.getQueryExpression(commandHelper.getCommandSpec())
            .map(QueryExpression::getExpression)
            .orElse(null);
    }
}
