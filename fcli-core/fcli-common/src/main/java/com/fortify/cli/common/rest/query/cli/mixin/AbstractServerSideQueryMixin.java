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
package com.fortify.cli.common.rest.query.cli.mixin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.output.cli.mixin.AbstractOutputHelperMixin;
import com.fortify.cli.common.output.query.IQueryExpressionSupplier;
import com.fortify.cli.common.output.query.QueryExpression;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.HttpRequest;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;

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
    
    private final Expression getSpelExpression() {
        QueryExpression queryExpression = getQueryExpression();
        return queryExpression==null ? null : queryExpression.getExpression();
    }
    private final QueryExpression getQueryExpression() {
        CommandSpec spec = commandHelper.getCommandSpec();
        CommandSpec outputHelperMixin = spec.mixins().get("outputHelper");
        if ( outputHelperMixin==null ) {
            throw new RuntimeException("Command must provide outputHelper mixin: "+spec.userObject().getClass().getName());
        }
        var mixinInstance = (AbstractOutputHelperMixin)outputHelperMixin.userObject();
        var outputWriterFactory = mixinInstance.getOutputWriterFactory();
        return outputWriterFactory instanceof IQueryExpressionSupplier 
                ? ((IQueryExpressionSupplier)outputWriterFactory).getQueryExpression()
                : null;
    }
}
