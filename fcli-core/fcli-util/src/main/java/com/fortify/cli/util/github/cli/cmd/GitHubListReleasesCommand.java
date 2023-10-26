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

import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import kong.unirest.HttpRequest;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "list-releases", aliases = "lsr")
public class GitHubListReleasesCommand extends AbstractGitHubRepoCommand implements IBaseRequestSupplier {
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;
    
    @Override
    public HttpRequest<?> getBaseRequest() {
        var endpoint = getRepoEndpointUrl("/releases");
        return getUnirestInstance().get(endpoint);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
