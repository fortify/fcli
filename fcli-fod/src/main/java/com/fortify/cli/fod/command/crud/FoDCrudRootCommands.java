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

import com.fortify.cli.common.picocli.command.crud.RootCreateCommand;
import com.fortify.cli.common.picocli.command.crud.RootDeleteCommand;
import com.fortify.cli.common.picocli.command.crud.RootGetCommand;
import com.fortify.cli.common.picocli.command.crud.RootUpdateCommand;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.fod.FoDConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class FoDCrudRootCommands {
	@ReflectiveAccess
		@Command(name = FoDConstants.PRODUCT_ID, description = "Get entity data from FoD")
	public static class FoDGetCommand {
		public static final OutputConfig defaultOutputConfig() {
			return RootGetCommand.defaultOutputConfig().inputTransformer(json->json.get("data"));
		}
	}
	
	@ReflectiveAccess
		@Command(name = FoDConstants.PRODUCT_ID, description = "Create entities in FoD")
	public static class FoDCreateCommand {
		public static final OutputConfig defaultOutputConfig() {
			return RootCreateCommand.defaultOutputConfig().inputTransformer(json->json.get("data"));
		}
	}
	
	@ReflectiveAccess
		@Command(name = FoDConstants.PRODUCT_ID, description = "Update entities in FoD")
	public static class FoDUpdateCommand {
		public static final OutputConfig defaultOutputConfig() {
			return RootUpdateCommand.defaultOutputConfig().inputTransformer(json->json.get("data"));
		}
	}
	
	@ReflectiveAccess
		@Command(name = FoDConstants.PRODUCT_ID, description = "Delete entities from FoD")
	public static class FoDDeleteCommand {
		public static final OutputConfig defaultOutputConfig() {
			return RootDeleteCommand.defaultOutputConfig().inputTransformer(json->json.get("data"));
		}
	}
}
