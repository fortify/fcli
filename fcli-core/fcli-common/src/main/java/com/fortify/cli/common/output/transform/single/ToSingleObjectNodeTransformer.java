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
package com.fortify.cli.common.output.transform.single;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.AbstractJsonNodeTransformer;

public class ToSingleObjectNodeTransformer extends AbstractJsonNodeTransformer {
    public final boolean failOnMultiple;
    
    public ToSingleObjectNodeTransformer(boolean failOnMultiple) {
        super(false);
        this.failOnMultiple = failOnMultiple;
    }
    
    @Override
    protected JsonNode transformObjectNode(ObjectNode input) {
        return input;
    }
    
    @Override
    protected JsonNode transformArrayNode(ArrayNode input) {
        if ( input==null || input.size()==0 ) {
            return null;
        } else if ( input.size()>1 && failOnMultiple ) {
            throw new IllegalArgumentException("Expected single JSON object, received multiple");
        } else {
            return input.get(0);
        }
    }

}
