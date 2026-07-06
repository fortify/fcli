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
import com.fortify.cli.common.http.ssl.truststore.helper.TrustedUrlCertificateDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustedUrlTrustStoreHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.config.truststore.helper.TrustStoreOutputHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "list-trusted-urls")
public class TrustStoreListTrustedUrlsCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;

    @Override
    public JsonNode getJsonNode() {
        return TrustedUrlTrustStoreHelper.listTrustedUrls()
            .map(TrustedUrlCertificateDescriptor::asJsonNode)
            .collect(JsonHelper.arrayNodeCollector());
    }

    @Override
    public boolean isSingular() {
        return false;
    }

    @Override
    public JsonNode transformRecord(JsonNode input) {
        return TrustStoreOutputHelper.transformRecord(input);
    }
}
