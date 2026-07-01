/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.fod.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Command to list users with access to an application.
 * @author Sangamesh Vijaykumar
 */
@Command(name = "list-users", aliases = "lsu") @CommandGroup("user")
@DefaultVariablePropertyName("userId")
public class FoDAppUserListCommand extends AbstractFoDBaseRequestOutputCommand implements IInputTransformer {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;
    @Mixin private FoDAppResolverMixin.RequiredOption appResolver;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(FoDUrls.APP_USERS)
                .routeParam("appId", appResolver.getAppId(unirest));
    }

    @Override
    public JsonNode transformInput(JsonNode input) {
        if ( input != null && input.has("users") ) { return input.get("users"); }
        return input;
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}