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
package com.fortify.cli.common.config.product;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.config.FcliConfig;

import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class EnabledProductsHelper {
	private static final String CONFIG_KEY = "enabled-products";
	private final FcliConfig config;
	
	@Inject
	public EnabledProductsHelper(FcliConfig config) {
		this.config = config;
	}
	
	public Set<Product> getEnabledProducts() {
		String configValue = config.get(CONFIG_KEY);
		return StringUtils.isEmpty(configValue)
			? new HashSet<>(Arrays.asList(Product.values()))
			: Stream.of(configValue.split(",")).map(Product::valueOf).collect(Collectors.toSet());
	}
	
	public void setEnabledProducts(Product[] products) {
		String configValue = Stream.of(products).flatMap(Product::thisAndDependenciesStream)
				.map(Product::name).collect(Collectors.joining( "," ));
		config.set(CONFIG_KEY, configValue);
	}

	public boolean isProductEnabled(Optional<Product> optProduct) {
		return optProduct.isEmpty() || getEnabledProducts().contains(optProduct.get());
	}
}
