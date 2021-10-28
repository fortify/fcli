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
package com.fortify.cli.ssc.command.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.RequiresProduct;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.config.product.Product;
import com.fortify.cli.ssc.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.command.entity.SSCEntityRootCommands.SSCGetCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;

public class SSCSystemEventCommands {
    private static final String NAME = "system-events";
    private static final String DESC = "system events";
	
    @ReflectiveAccess
	@SubcommandOf(SSCGetCommand.class)
	@Command(name = NAME, description = "Get "+DESC+" data from SSC")
	@RequiresProduct(Product.SSC)
	public static final class Get extends AbstractSSCUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.out.println(
				unirest.get("/api/v1/events?limit=-1")
					.accept("application/json")
					.header("Content-Type", "application/json")
					.asObject(ObjectNode.class)
					.getBody()
					.get("data")
					.toPrettyString()
			);
			
			return null;
		}
	}
}
