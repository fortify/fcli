/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.ssc._common.rest.sc_sast.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;

// IMPORTANT: When updating/adding any methods in this class, SCSastControllerRestCallCommand
//            also likely needs to be updated
public class SCSastProductHelper implements IProductHelper, IInputTransformer
{
    public static final SCSastProductHelper INSTANCE = new SCSastProductHelper();
    private SCSastProductHelper() {}
    @Override
    public JsonNode transformInput(JsonNode input) {
        return SCSastInputTransformer.getItems(input);
    }
}