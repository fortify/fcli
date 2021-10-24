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
package com.fortify.cli.ssc.command.entity;

import com.fortify.cli.common.command.util.SubcommandOf;
import com.fortify.cli.common.command.entity.RootApiCommand.*;
import java.lang.String;


import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

public class SSCApiCommands {
    private static final String NAME = "ssc";
	private static final String DESC = "Fortify SSC REST API";
    
    private static void workInProgress(String from){
        System.out.println(String.format("Work in progress. (%s)", from));
    }

    @Singleton
	@SubcommandOf(ApiGetCommand.class)
	@Command(name = NAME, description = "GET data by calling a "+DESC)
	public static final class Get implements Runnable{
        @Override
        public void run() { workInProgress("GET SSC"); }
	}

    @Singleton
	@SubcommandOf(ApiPostCommand.class)
	@Command(name = NAME, description = "POST data to a "+DESC)
	public static final class Post implements Runnable{
		@Override
        public void run() { workInProgress("POST SSC"); }
	}

    @Singleton
	@SubcommandOf(ApiPutCommand.class)
	@Command(name = NAME, description = "PUT/Update data to a "+DESC)
	public static final class Put implements Runnable{
		@Override
        public void run() { workInProgress("PUT SSC"); }
	}

    @Singleton
	@SubcommandOf(ApiDeleteCommand.class)
	@Command(name = NAME, description = "DELETE data from a "+DESC)
	public static final class Delete implements Runnable{
		@Override
        public void run() { workInProgress("DELETE SSC"); }
	}


}
