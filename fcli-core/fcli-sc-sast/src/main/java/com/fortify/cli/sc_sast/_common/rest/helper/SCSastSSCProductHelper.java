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
package com.fortify.cli.sc_sast._common.rest.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.ssc._common.rest.helper.SSCInputTransformer;
import com.fortify.cli.ssc._common.rest.helper.SSCPagingHelper;

public class SCSastSSCProductHelper implements IProductHelper, IInputTransformer, INextPageUrlProducerSupplier
{
    public static final SCSastSSCProductHelper INSTANCE = new SCSastSSCProductHelper();
    private SCSastSSCProductHelper() {}
    
    @Override
    public JsonNode transformInput(JsonNode input) {
        return SSCInputTransformer.getDataOrSelf(input);
    }
    
    @Override
    public INextPageUrlProducer getNextPageUrlProducer() {
        return SSCPagingHelper.nextPageUrlProducer();
    }
}