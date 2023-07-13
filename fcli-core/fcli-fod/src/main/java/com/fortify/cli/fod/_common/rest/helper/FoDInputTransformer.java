/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.fod._common.rest.helper;

import com.fasterxml.jackson.databind.JsonNode;

public class FoDInputTransformer {
    public static final JsonNode getItems(JsonNode input) {
        if ( input != null && input.has("items") ) { return input.get("items"); }
        return input;
    }
}
