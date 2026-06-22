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
package com.fortify.cli.config.truststore.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustedUrlTrustStoreHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.config.truststore.helper.TrustStoreOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = "add-trusted-url")
public class TrustStoreAddTrustedUrlCommand extends AbstractOutputCommand
    implements IJsonNodeSupplier, IActionCommandResultSupplier, IRecordTransformer
{
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;

    @Parameters(index = "0", paramLabel = "<url>", descriptionKey = "fcli.config.truststore.add-trusted-url.url")
    private String url;

    @Override
    public JsonNode getJsonNode() {
        return TrustedUrlTrustStoreHelper.addTrustedUrl(url).asJsonNode();
    }

    @Override
    public String getActionCommandResult() {
        return "ADDED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public JsonNode transformRecord(JsonNode input) {
        return TrustStoreOutputHelper.transformRecord(input);
    }
}
