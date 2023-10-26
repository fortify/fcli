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
package com.fortify.cli.util.github.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.HttpRequest;
import kong.unirest.RawResponse;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "list-release-assets", aliases = "lsra")
public class GitHubListReleaseAssetsCommand extends AbstractGitHubRepoCommand implements IBaseRequestSupplier, IInputTransformer, IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    @Option(names = {"--digest", "-d"}, required = false) private String digestAlgorithm;
    
    @Override
    public HttpRequest<?> getBaseRequest() {
        var endpoint = getRepoEndpointUrl("/releases");
        return getUnirestInstance().get(endpoint);
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return addDigest((ObjectNode)record);
    }
    
    @Override
    public JsonNode transformInput(JsonNode input) {
        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        input.forEach(release->addReleaseAssets(result, (ObjectNode)release));
        return result;
    }
    
    private void addReleaseAssets(ArrayNode result, ObjectNode release) {
        release.get("assets").forEach(asset->addReleaseAsset(result, release, (ObjectNode)asset));
        release.remove("assets");
    }

    private void addReleaseAsset(ArrayNode result, ObjectNode release, ObjectNode asset) {
        asset.set("release", release);
        // We already filter assets here, to avoid calculating hashes for non-matching assets
        if ( outputHelper.getOutputWriterFactory().getQueryExpression().matches(asset) ) {
            result.add(asset);
        }
    }

    private ObjectNode addDigest(ObjectNode asset) {
        String digest_algorithm = "N/A";
        String digest = "Not Calculated"; 
        if ( StringUtils.isNotBlank(digestAlgorithm) ) {
            try (var pw = progressWriterFactory.create()) {
                var downloadUrl = asset.get("browser_download_url").asText();
                pw.writeProgress("Calculating digest for %s", downloadUrl);
                digest_algorithm = this.digestAlgorithm;
                digest = getUnirestInstance().get(downloadUrl).asObject(raw->calculateDigest(downloadUrl, raw)).getBody();
            }
        }
        asset.put("digest_algorithm", digest_algorithm);
        asset.put("digest", digest);
        return asset;
    }
    
    private String calculateDigest(String downloadUrl, RawResponse rawResponse) {
        return FileUtils.getDigest(downloadUrl, rawResponse.getContent(), digestAlgorithm);
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
