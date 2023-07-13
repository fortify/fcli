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
package com.fortify.cli.ssc._common.output.cli.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc._common.rest.helper.SSCInputTransformer;
import com.fortify.cli.ssc._common.rest.helper.SSCPagingHelper;
import com.fortify.cli.ssc._common.session.helper.SSCSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCSessionHelper;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class SSCProductHelperMixin extends AbstractSessionUnirestInstanceSupplierMixin<SSCSessionDescriptor>
    implements IProductHelper, IInputTransformer, INextPageUrlProducerSupplier
{
    @Getter private UnaryOperator<JsonNode> inputTransformer = SSCInputTransformer::getDataOrSelf;
    
    @Override
    public INextPageUrlProducer getNextPageUrlProducer(HttpRequest<?> originalRequest) {
        return SSCPagingHelper.nextPageUrlProducer();
    }
    
    @Override
    public JsonNode transformInput(JsonNode input) {
        return SSCInputTransformer.getDataOrSelf(input);
    }
    
    @Override
    public final SSCSessionDescriptor getSessionDescriptor(String sessionName) {
        return SSCSessionHelper.instance().get(sessionName, true);
    }
    
    @Override
    public final void configure(UnirestInstance unirest, SSCSessionDescriptor sessionDescriptor) {
        SSCTokenHelper.configureUnirest(unirest, sessionDescriptor.getUrlConfig(), sessionDescriptor.getActiveToken());
    }
}
