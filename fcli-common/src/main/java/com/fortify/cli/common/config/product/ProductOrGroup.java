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

import java.util.stream.Stream;

import lombok.Getter;

/**
 * This enum lists all (server) products currently supported by fcli. 
 * 
 * @author Ruud Senden
 */
public enum ProductOrGroup {
	SSC(ProductIdentifiers.SSC),
	FOD(ProductIdentifiers.FOD),
	SC_SAST(ProductIdentifiers.SC_SAST),
	SC_DAST(ProductIdentifiers.SC_DAST, SSC),
	ALL("all", SSC, FOD, SC_SAST, SC_DAST),
	ON_PREM("all-on-prem", SSC, SC_SAST, SC_DAST),
	CIRRUS("all-cirrus", SSC, SC_SAST, SC_DAST)
	;
	
	public static class ProductIdentifiers {
        public static final String SSC     = "ssc";
        public static final String FOD     = "fod";
        public static final String SC_DAST = "sc-dast";
        public static final String SC_SAST = "sc-sast";
    }
	
	@Getter private final String identifier;
	@Getter private final ProductOrGroup[] dependentOn;
	@Getter private final ProductOrGroup[] thisAndDependencies;
	ProductOrGroup(String identifier, ProductOrGroup... dependentOn) {
		this.identifier = identifier;
		this.dependentOn = dependentOn!=null ? dependentOn : new ProductOrGroup[] {};
		this.thisAndDependencies = _getThisAndDependencies();
	}
	
	private final ProductOrGroup[] _getThisAndDependencies() {
		return Stream.concat(Stream.of(this), Stream.of(dependentOn)).toArray(ProductOrGroup[]::new);
	}
	
	public final Stream<ProductOrGroup> thisAndDependenciesStream() {
		return Stream.of(thisAndDependencies);
	}

	public static final ProductOrGroup valueOfIdentifier(String identifier) {
		return Stream.of(ProductOrGroup.values())
			.filter(p->p.getIdentifier().equals(identifier))
			.findFirst()
			.orElseThrow();
	}
	
	public static final String[] identifiers() {
		return Stream.of(ProductOrGroup.values())
			.map(ProductOrGroup::getIdentifier)
			.toArray(String[]::new);
	}
}
