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
package com.fortify.cli.common.picocli.executor;

import java.util.Optional;

import com.fortify.cli.common.config.alpha.AlphaFeaturesHelper;
import com.fortify.cli.common.config.product.EnabledProductsHelper;
import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.picocli.annotation.AlphaFeature;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.BeanDefinition;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;

@Singleton
public class EnabledCommandBeansHelper {
	@Getter private final AlphaFeaturesHelper alphaFeaturesHelper;
	@Getter private final EnabledProductsHelper enabledProductsHelper;
	
	@Inject
	public EnabledCommandBeansHelper(
			AlphaFeaturesHelper alphaFeaturesHelper,
			EnabledProductsHelper enabledProductsHelper) {
		this.alphaFeaturesHelper = alphaFeaturesHelper;
		this.enabledProductsHelper = enabledProductsHelper;
	}
	
	public final boolean isEnabled(BeanDefinition<?> bd) {
		return isRequiredProductEnabled(bd) && isNotAlphaOrAllowed(bd);
	}

	public final boolean isNotAlphaOrAllowed(BeanDefinition<?> bd) {
		return !bd.hasAnnotation(AlphaFeature.class) || isAlphaFeaturesEnabled();	
	}

	public final boolean isAlphaFeaturesEnabled() {
		return alphaFeaturesHelper.isAlphaFeaturesEnabled();
	}
	
	public final boolean isRequiredProductEnabled(BeanDefinition<?> bd) {
		boolean result = true;
		AnnotationValue<RequiresProduct> annotation = bd.getAnnotation(RequiresProduct.class);
		if ( annotation!=null ) {
			Optional<ProductOrGroup> productOrGroup = annotation.enumValue(ProductOrGroup.class);
			result = enabledProductsHelper.isProductEnabled(productOrGroup);
		}
		return result;
	}
	
	public final ProductOrGroup getRequiredProduct(BeanDefinition<?> bd) {
		ProductOrGroup result = null;
		AnnotationValue<RequiresProduct> annotation = bd.getAnnotation(RequiresProduct.class);
		if ( annotation!=null ) {
			result = annotation.enumValue(ProductOrGroup.class).get();
		}
		return result;
	}

	public String getDisabledReason(BeanDefinition<?> bd) {
		if ( !isRequiredProductEnabled(bd) ) {
			return String.format("Required product '%s' not enabled; please see 'fcli config enabled-products'", getRequiredProduct(bd));
		} else if ( !isNotAlphaOrAllowed(bd) ) {
			return "Alpha features not enabled; please see 'fcli config enable-alpha-features'";
		} else {
			return null;
		}
	}
}
