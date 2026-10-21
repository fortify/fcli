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
package com.fortify.cli.aviator.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.app.cli.mixin.AviatorAppResolverMixin;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "reduce-entitlement")
public class AviatorAppReduceEntitlementCommand extends AbstractAviatorAdminSessionOutputCommand
        implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private AviatorAppResolverMixin.NameOrIdParameter appResolver;

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String applicationId = appResolver.getApplicationId(client, configDescriptor);
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(configDescriptor,
                    configDescriptor.getTenant(), applicationId, "reduce-entitlement");
            return AviatorGrpcUtils.grpcToJsonNode(client.reduceEntitlement(applicationId, messageAndSignature[1],
                    messageAndSignature[0], configDescriptor.getTenant()));
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }
}