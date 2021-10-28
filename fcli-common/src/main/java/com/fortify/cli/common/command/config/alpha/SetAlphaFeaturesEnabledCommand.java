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
package com.fortify.cli.common.command.config.alpha;

import com.fortify.cli.common.command.config.RootConfigCommand;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.config.alpha.AlphaFeaturesHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

// TODO Add list of products to help output
// TODO Add completionCandidates
@ReflectiveAccess
@SubcommandOf(RootConfigCommand.class)
@Command(name = "enable-alpha-features", description = {
		"Configure whether alpha features of fcli are enabled or not.",
		"Please be aware that alpha features may be highly unstable",
		"or not functioning at all, and may never make it to an actual",
		"production-level feature."
		})
public class SetAlphaFeaturesEnabledCommand implements Runnable {
	private final AlphaFeaturesHelper helper;
	
	@Parameters(index = "0", arity = "1..1", description = "Valid values: true, false")
	private boolean alphaFeaturesEnabled;
	
	@Inject
	public SetAlphaFeaturesEnabledCommand(AlphaFeaturesHelper helper) {
		this.helper = helper;
	}
	
	@Override
	public void run() {
		helper.setAlphaFeaturesEnabled(alphaFeaturesEnabled);
	}
	/*
	private static final class ProductConverter implements ITypeConverter<Product> {
		@Override
		public Product convert(String value) throws Exception {
			return Product.valueOfIdentifier(value);
		}
	}
	
	private static final class ProductCompletionCandidates implements Iterable<String> {
		@Override
		public Iterator<String> iterator() {
			return Arrays.asList(Product.identifiers()).iterator();
		}
	}
	*/
}
