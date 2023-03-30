package com.fortify.cli.ssc.rest.query.cli.mixin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.rest.cli.mixin.AbstractServerQueryMixin;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.rest.query.ISSCQParamGeneratorSupplier;

import kong.unirest.HttpRequest;
import picocli.CommandLine.Option;

public class SSCQParamMixin extends AbstractServerQueryMixin implements IHttpRequestUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(SSCQParamMixin.class);
    @Option(names="--q-param", required=false, descriptionKey="fcli.ssc.q-param")
    private String qParam;
    
    @Override
    public HttpRequest<?> updateRequest(HttpRequest<?> request) {
        String _qParam = this.qParam;
        if ( StringUtils.isBlank(_qParam) ) {
            ISSCQParamGeneratorSupplier qParamGeneratorSupplier = 
                    getCommandHelper().getCommandAs(ISSCQParamGeneratorSupplier.class);
            _qParam = qParamGeneratorSupplier==null 
                    ? null 
                    : qParamGeneratorSupplier.getQParamGenerator().getQParamValue(getQueryExpression());
        }
        if ( StringUtils.isBlank(_qParam) ) {
            LOG.debug("Not adding q parameter");
            return request;
        } else {
            LOG.debug("Adding q parameter with value: {}", _qParam);
            return request.queryString("q", _qParam);
        }
    }
}
