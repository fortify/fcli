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
package com.fortify.cli;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.fortify.cli.common.command.log.LogOptionsMixin;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class FortifyCLILogHelper {
	private static final PrintWriter DUMMY_WRITER = new PrintWriter(new StringWriter());
	public static final void configureLogging(String[] args) {
		CommandLine commandLine = new CommandLine(SetupLoggingCommand.class)
				.setOut(DUMMY_WRITER)
				.setErr(DUMMY_WRITER)
				.setUnmatchedArgumentsAllowed(true)
				.setUnmatchedOptionsArePositionalParams(true)
				.setExpandAtFiles(true);
		commandLine.execute(args);
				
		// TODO Do an initial parse of the command line arguments to configure logging
		//      This could be either a simple for-loop to find logging-specific options,
		//      or could utilize a simple Picocli instance that's configured to ignore
		//      unknown arguments. Note that any logging options should also be added to
		//      FCLIRootCommand (without actually doing anything with them)
	}
	
	@Command()
	public static final class SetupLoggingCommand implements Runnable {
		@Mixin LogOptionsMixin logOptionsMixin;
		
		@Override
		public void run() {
			logOptionsMixin.configureLogging();
		}
	}
}
