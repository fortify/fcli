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
package com.fortify.cli.fod.command.entity;

import com.fortify.cli.common.command.util.annotation.RequiresProduct;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.config.product.Product;
import com.fortify.cli.fod.command.entity.FoDEntityRootCommands.FoDCreateCommand;
import com.fortify.cli.fod.command.entity.FoDEntityRootCommands.FoDDeleteCommand;
import com.fortify.cli.fod.command.entity.FoDEntityRootCommands.FoDGetCommand;
import com.fortify.cli.fod.command.entity.FoDEntityRootCommands.FoDUpdateCommand;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

public class FoDApplicationEntityCommands {
	private static final String ENTITY = "application";
	
	@Singleton
	@SubcommandOf(FoDGetCommand.class)
	@Command(name = ENTITY, description = "Get "+ENTITY+" data from FoD")
	@RequiresProduct(Product.FOD)
	public static final class Get implements Runnable {
		@Override
		public void run() {
		}
	}
	
	@Singleton
	@SubcommandOf(FoDCreateCommand.class)
	@Command(name = ENTITY, description = "Create "+ENTITY+" in FoD")
	@RequiresProduct(Product.FOD)
	public static final class Create implements Runnable {
		@Override
		public void run() {
		}
	}
	
	@Singleton
	@SubcommandOf(FoDUpdateCommand.class)
	@Command(name = ENTITY, description = "Update "+ENTITY+" in FoD")
	@RequiresProduct(Product.FOD)
	public static final class Update implements Runnable {
		@Override
		public void run() {
		}
	}
	
	@Singleton
	@SubcommandOf(FoDDeleteCommand.class)
	@Command(name = ENTITY, description = "Delete "+ENTITY+" from FoD")
	@RequiresProduct(Product.FOD)
	public static final class Delete implements Runnable {
		@Override
		public void run() {
		}
	}
}
