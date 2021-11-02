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
package com.fortify.cli.fod.command.crud;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.crud.RootCreateCommand;
import com.fortify.cli.common.picocli.command.crud.RootDeleteCommand;
import com.fortify.cli.common.picocli.command.crud.RootGetCommand;
import com.fortify.cli.common.picocli.command.crud.RootUpdateCommand;
import com.fortify.cli.common.picocli.component.output.OutputOptionsWriterConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class FoDCrudRootCommands {
	@ReflectiveAccess
	@SubcommandOf(RootGetCommand.class)
	@Command(name = ProductIdentifiers.FOD, description = "Get entity data from FoD")
	@RequiresProduct(ProductOrGroup.FOD)
	public static class FoDGetCommand {
		public static final OutputOptionsWriterConfig defaultOutputConfig() {
			return RootGetCommand.defaultOutputConfig().inputTransformer(json->json.get("data"));
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(RootCreateCommand.class)
	@Command(name = ProductIdentifiers.FOD, description = "Create entities in FoD")
	@RequiresProduct(ProductOrGroup.FOD)
	public static class FoDCreateCommand {
		public static final OutputOptionsWriterConfig defaultOutputConfig() {
			return RootCreateCommand.defaultOutputConfig().inputTransformer(json->json.get("data"));
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(RootUpdateCommand.class)
	@Command(name = ProductIdentifiers.FOD, description = "Update entities in FoD")
	@RequiresProduct(ProductOrGroup.FOD)
	public static class FoDUpdateCommand {
		public static final OutputOptionsWriterConfig defaultOutputConfig() {
			return RootUpdateCommand.defaultOutputConfig().inputTransformer(json->json.get("data"));
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(RootDeleteCommand.class)
	@Command(name = ProductIdentifiers.FOD, description = "Delete entities from FoD")
	@RequiresProduct(ProductOrGroup.FOD)
	public static class FoDDeleteCommand {
		public static final OutputOptionsWriterConfig defaultOutputConfig() {
			return RootDeleteCommand.defaultOutputConfig().inputTransformer(json->json.get("data"));
		}
	}
}
