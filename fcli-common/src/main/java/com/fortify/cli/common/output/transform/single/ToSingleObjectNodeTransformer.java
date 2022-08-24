/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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
