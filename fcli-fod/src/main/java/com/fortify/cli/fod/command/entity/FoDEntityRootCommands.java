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

import com.fortify.cli.common.command.entity.RootCreateCommand;
import com.fortify.cli.common.command.entity.RootDeleteCommand;
import com.fortify.cli.common.command.entity.RootDownloadCommand;
import com.fortify.cli.common.command.entity.RootGetCommand;
import com.fortify.cli.common.command.entity.RootUpdateCommand;
import com.fortify.cli.common.command.entity.RootUploadCommand;
import com.fortify.cli.common.command.util.SubcommandOf;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

public class FoDEntityRootCommands {
	@Singleton
	@SubcommandOf(RootGetCommand.class)
	@Command(name = "fod", description = "Get entity data from FoD")
	public static final class FoDGetCommand {}
	
	@Singleton
	@SubcommandOf(RootCreateCommand.class)
	@Command(name = "fod", description = "Create entities in FoD")
	public static final class FoDCreateCommand {}
	
	@Singleton
	@SubcommandOf(RootUpdateCommand.class)
	@Command(name = "fod", description = "Update entities in FoD")
	public static final class FoDUpdateCommand {}
	
	@Singleton
	@SubcommandOf(RootDeleteCommand.class)
	@Command(name = "fod", description = "Delete entities from FoD")
	public static final class FoDDeleteCommand {}
	
	@Singleton
	@SubcommandOf(RootUploadCommand.class)
	@Command(name = "fod", description = "Upload data FoD")
	public static final class FoDUploadCommand {}
	
	@Singleton
	@SubcommandOf(RootDownloadCommand.class)
	@Command(name = "fod", description = "Download data from FoD")
	public static final class FoDDownloadCommand {}
}
