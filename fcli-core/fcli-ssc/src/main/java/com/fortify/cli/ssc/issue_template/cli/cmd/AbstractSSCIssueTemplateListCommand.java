package com.fortify.cli.ssc.issue_template.cli.cmd;

import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.query.SSCQParamGenerator;
import com.fortify.cli.ssc._common.rest.ssc.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc._common.rest.ssc.query.cli.mixin.SSCQParamMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

public abstract class AbstractSSCIssueTemplateListCommand extends AbstractSSCBaseRequestOutputCommand implements IServerSideQueryParamGeneratorSupplier {
    @Mixin protected SSCQParamMixin qParamMixin;
    protected IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new SSCQParamGenerator()
                .add("id", SSCQParamValueGenerators::plain)
                .add("name", SSCQParamValueGenerators::wrapInQuotes)
                .add("defaultTemplate", SSCQParamValueGenerators::plain)
                .add("applicationVersionId", "projectVersionId", SSCQParamValueGenerators::wrapInQuotes)
                .add("required", SSCQParamValueGenerators::plain);
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(SSCUrls.ISSUE_TEMPLATES);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    @Override
    public IServerSideQueryParamValueGenerator getServerSideQueryParamGenerator() {
        return serverSideQueryParamGenerator;
    }
}
