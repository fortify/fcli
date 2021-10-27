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
package com.fortify.cli.common.command.util.output;

import com.fortify.cli.common.json.mapper.FieldBasedTransformer;
import com.fortify.cli.common.json.mapper.FieldBasedTransformerFactory;
import com.fortify.cli.common.json.mapper.IJsonNodeTransformer;
import com.fortify.cli.common.json.mapper.IdentityTransformer;
import com.fortify.cli.common.output.OutputFormat;

public class AbstractJsonNodeTransformerSupplier implements IJsonNodeTransformerSupplier {

	@Override
	public IJsonNodeTransformer getJsonNodeTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
		switch (format.getOutputType()) {
		case TECHNICAL:    return createTechnicalTransformer(fieldBasedTransformerFactory, format);
		case TEXT_COLUMNS: return createToColumnsTransformer(fieldBasedTransformerFactory, format);
		case TEXT_ROWS:    return createToRowsTransformer(fieldBasedTransformerFactory, format);
		default:           return new IdentityTransformer();
		}
	}

	protected IdentityTransformer createTechnicalTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
		return new IdentityTransformer();
	}
	
	protected IJsonNodeTransformer createToColumnsTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
		FieldBasedTransformer transformer = fieldBasedTransformerFactory.createEmpty(format.getFieldNameFormatter());
		addColumns(format, transformer);
		return transformer;
	}
	
	protected void addColumns(OutputFormat format, FieldBasedTransformer transformer) {
		throw new RuntimeException("Either createToColumnsTransformer or addColumns method must be overriden");
	}

	protected IJsonNodeTransformer createToRowsTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
		return new IdentityTransformer();
	}
	
	
	
	
}
