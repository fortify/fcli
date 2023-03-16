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
package com.fortify.cli.common.cli.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.util.FixInjection;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(defaultValueProvider = FortifyCLIDefaultValueProvider.class)
@FixInjection
public class FortifyCLIInitializerRunner {
    private static final PrintWriter DUMMY_WRITER = new PrintWriter(new StringWriter());
    
    public static final void initialize(String[] args, MicronautFactory micronautFactory) {
        // Remove help options, as we want initialization always to occur
        String[] argsWithoutHelp = Stream.of(args).filter(a->!a.matches("-h|--help")).toArray(String[]::new);
        new CommandLine(FortifyCLIInitializerCommand.class, micronautFactory)
                .setOut(DUMMY_WRITER)
                .setErr(DUMMY_WRITER)
                .setUnmatchedArgumentsAllowed(true)
                .setUnmatchedOptionsArePositionalParams(true)
                .setExpandAtFiles(true)
                .execute(argsWithoutHelp);
    }
    
    @Command(name = "fcli", defaultValueProvider = FortifyCLIDefaultValueProvider.class)
    @FixInjection
    public static final class FortifyCLIInitializerCommand extends AbstractFortifyCLICommand implements Runnable {
        @Inject @ReflectiveAccess private IFortifyCLIInitializer[] initializers;
        @Getter @Spec private CommandSpec commandSpec;
        
        @Override
        public void run() {
            for ( var initializer : initializers ) {
                initializer.initializeFortifyCLI(this);
            }
        }
    }
}
