/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.util.github.cli.cmd;

import org.apache.http.client.utils.URIBuilder;

import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.rest.github.GitHubPagingHelper;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;

/**
 *
 * @author Ruud Senden
 */
public abstract class AbstractGitHubRepoCommand extends AbstractOutputCommand implements INextPageUrlProducerSupplier, IUnirestInstanceSupplier {
    @Getter @Option(names={"--api-url"}, defaultValue = "https://api.github.com", required = true, descriptionKey = "fcli.util.github.api-url") 
    private String apiUrl;
    @Getter @Option(names={"--repo", "-r"}, required = true, descriptionKey = "fcli.util.github.repo") 
    private String repo;
    
    @Override
    public UnirestInstance getUnirestInstance() {
        return GenericUnirestFactory.getUnirestInstance(apiUrl, null);
    }

    @Override
    public INextPageUrlProducer getNextPageUrlProducer() {
        return GitHubPagingHelper.nextPageUrlProducer();
    }
    
    @SneakyThrows
    protected String getRepoEndpointUrl(String endpoint) {
        endpoint = endpoint.replaceAll("^/", "");
        return new URIBuilder(apiUrl).setPath(String.format("/repos/%s/%s", repo, endpoint)).build().toString();
    }
}