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
package com.fortify.cli.fod.action.cli.cmd;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fortify.cli.common.action.cli.cmd.AbstractActionRunCommand;
import com.fortify.cli.common.action.runner.ActionRunnerConfig.ActionRunnerConfigBuilder;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.action.helper.FoDActionSpelFunctions;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "run")
public class FoDActionRunCommand extends AbstractActionRunCommand {
    @Getter @Mixin private FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;
    
    @Override
    protected final String getType() {
        return "FoD";
    }
    
    @Override
    protected void configure(ActionRunnerConfigBuilder configBuilder) {
    configBuilder
            .defaultFcliRunOption("--fod-session", unirestInstanceSupplier.getSessionName())
            .actionContextConfigurer(this::configureActionContext)
            .actionContextSpelEvaluatorConfigurer(this::configureSpelContext);
    }
    
    protected void configureActionContext(ActionRunnerContext ctx) {
        ctx.addRequestHelper("fod", new FoDDataExtractRequestHelper(unirestInstanceSupplier::getUnirestInstance, FoDProductHelper.INSTANCE));
    }
    
    protected void configureSpelContext(ActionRunnerContext actionRunnerContext, SimpleEvaluationContext spelContext) {
        spelContext.setVariable("fod", new FoDActionSpelFunctions(unirestInstanceSupplier, actionRunnerContext));
    }
    
    private static final class FoDDataExtractRequestHelper extends BasicActionRequestHelper {
        public FoDDataExtractRequestHelper(IUnirestInstanceSupplier unirestInstanceSupplier, IProductHelper productHelper) {
            super(unirestInstanceSupplier, productHelper);
        }
    }
}
