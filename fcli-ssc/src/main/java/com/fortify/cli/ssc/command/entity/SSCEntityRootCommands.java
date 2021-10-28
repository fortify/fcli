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

import com.fortify.cli.common.command.entity.RootCreateCommand;
import com.fortify.cli.common.command.entity.RootDeleteCommand;
import com.fortify.cli.common.command.entity.RootDownloadCommand;
import com.fortify.cli.common.command.entity.RootGetCommand;
import com.fortify.cli.common.command.entity.RootUpdateCommand;
import com.fortify.cli.common.command.entity.RootUploadCommand;
import com.fortify.cli.common.command.util.annotation.RequiresProduct;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.config.product.Product;
import com.fortify.cli.common.config.product.Product.ProductIdentifiers;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class SSCEntityRootCommands {
	@ReflectiveAccess
	@SubcommandOf(RootGetCommand.class)
	@Command(name = ProductIdentifiers.SSC, description = "Get entity data from SSC")
	@RequiresProduct(Product.SSC)
	public static class SSCGetCommand {}
	
	@ReflectiveAccess
	@SubcommandOf(RootCreateCommand.class)
	@Command(name = ProductIdentifiers.SSC, description = "Create entities in SSC")
	@RequiresProduct(Product.SSC)
	public static class SSCCreateCommand {}
	
	@ReflectiveAccess
	@SubcommandOf(RootUpdateCommand.class)
	@Command(name = ProductIdentifiers.SSC, description = "Update entities in SSC")
	@RequiresProduct(Product.SSC)
	public static class SSCUpdateCommand {}
	
	@ReflectiveAccess
	@SubcommandOf(RootDeleteCommand.class)
	@Command(name = ProductIdentifiers.SSC, description = "Delete entities from SSC")
	@RequiresProduct(Product.SSC)
	public static class SSCDeleteCommand {}
	
	@ReflectiveAccess
	@SubcommandOf(RootUploadCommand.class)
	@Command(name = ProductIdentifiers.SSC, description = "Upload data SSC")
	@RequiresProduct(Product.SSC)
	public static class SSCUploadCommand {}
	
	@ReflectiveAccess
	@SubcommandOf(RootDownloadCommand.class)
	@Command(name = ProductIdentifiers.SSC, description = "Download data from SSC")
	@RequiresProduct(Product.SSC)
	public static class SSCDownloadCommand {}
}
