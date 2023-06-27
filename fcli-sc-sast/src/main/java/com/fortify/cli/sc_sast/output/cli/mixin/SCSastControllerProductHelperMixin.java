/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_sast.output.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.sc_sast.rest.helper.SCSastInputTransformer;
import com.fortify.cli.sc_sast.session.cli.mixin.AbstractSCSastUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;

public class SCSastControllerProductHelperMixin extends AbstractSCSastUnirestInstanceSupplierMixin
    implements IProductHelper, IInputTransformer, IUnirestInstanceSupplier
{
    @Override
    public JsonNode transformInput(JsonNode input) {
        return SCSastInputTransformer.getItems(input);
    }
    
    @Override
    public UnirestInstance getUnirestInstance() {
        return getControllerUnirestInstance();
    }
}