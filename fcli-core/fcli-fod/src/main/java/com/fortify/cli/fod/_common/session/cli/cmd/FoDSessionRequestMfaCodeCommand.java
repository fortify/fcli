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
package com.fortify.cli.fod._common.session.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.output.cli.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.session.cli.mixin.FoDSessionLoginOptions;
import com.fortify.cli.fod._common.session.helper.FoDMfaDeliveryType;
import com.fortify.cli.fod._common.session.helper.FoDMfaHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Command for requesting a Multi-Factor Authentication (MFA) code via Email or SMS.
 * @author Sangamesh Vijaykumar
 */
@Command(name = FoDOutputHelperMixins.RequestMfaCode.CMD_NAME, sortOptions = false)
public class FoDSessionRequestMfaCodeCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.RequestMfaCode outputHelper;
    @Mixin private FoDSessionLoginOptions.FoDUrlConfigOptions urlConfigOptions;
    @Mixin private FoDSessionLoginOptions.FoDUserCredentialOptions userCredentials;
    @Option(names = {"--delivery-mode", "-m"}, required = true)
    private FoDMfaDeliveryType deliveryMode;

    @Override
    public JsonNode getJsonNode() {
        FoDMfaHelper.requestMfaCode(
            urlConfigOptions,
            userCredentials,
            deliveryMode
        );
        
        String fodUrl = FoDProductHelper.INSTANCE.getBrowserUrl(urlConfigOptions.getUrl());
        
        ObjectNode result = com.fortify.cli.common.json.JsonHelper.getObjectMapper().createObjectNode();
        result.put("fodUrl", fodUrl);
        result.put("deliveryMode", deliveryMode.name());
        return result;
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return "REQUESTED";
    }
}
