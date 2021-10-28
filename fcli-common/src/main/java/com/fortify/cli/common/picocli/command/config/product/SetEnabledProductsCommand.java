/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.common.picocli.command.config.product;

import java.util.Arrays;
import java.util.Iterator;

import com.fortify.cli.common.config.product.EnabledProductsHelper;
import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.config.RootConfigCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@SubcommandOf(RootConfigCommand.class)
@Command(name = "enabled-products", description = {
		"Configure the products for which commands should be enabled in fcli.",
		"By default, fcli will display all commands and options; configuring",
		"this option allows for hiding commands and options for products that",
		"you are not using."
		})
public class SetEnabledProductsCommand implements Runnable {
	private final EnabledProductsHelper helper;
	
	@Parameters(index = "0", arity = "1..", 
			converter = ProductConverter.class, 
			completionCandidates = ProductCompletionCandidates.class,
			description = "Valid values: ${COMPLETION-CANDIDATES}" )
	private ProductOrGroup[] enabledProducts;
	
	@Inject
	public SetEnabledProductsCommand(EnabledProductsHelper helper) {
		this.helper = helper;
	}
	
	@Override
	public void run() {
		helper.setEnabledProducts(enabledProducts);
	}
	
	private static final class ProductConverter implements ITypeConverter<ProductOrGroup> {
		@Override
		public ProductOrGroup convert(String value) throws Exception {
			return ProductOrGroup.valueOfIdentifier(value);
		}
	}
	
	private static final class ProductCompletionCandidates implements Iterable<String> {
		@Override
		public Iterator<String> iterator() {
			return Arrays.asList(ProductOrGroup.identifiers()).iterator();
		}
	}
}
