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
package com.fortify.cli.ssc.picocli.command.appversion;

import com.ctc.wstx.shaded.msv_core.util.LightStack;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.ssc.picocli.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.picocli.mixin.filter.SSCFilterMixin;
import com.fortify.cli.ssc.util.SSCOutputHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import org.fusesource.jansi.AnsiConsole;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

@ReflectiveAccess
@Command(name = "create-wizard")
public class SSCAppVersionCreateWizardCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
	@CommandLine.Mixin private OutputMixin outputMixin;
	@CommandLine.Mixin private SSCFilterMixin sscFilterMixin;

	private void jline3Test() throws IOException {
		System.setProperty("org.jline.terminal.dumb", "true");

		// JANSI test
		AnsiConsole.systemInstall();

		// JLine3 test
		Terminal terminal = TerminalBuilder.terminal();
		terminal.enterRawMode();

		LineReader reader = LineReaderBuilder.builder()
				.terminal(terminal)
				.build();

		ArrayList<String> options = new ArrayList<>();
		options.add("option-1");
		options.add("option-2");
		options.add("option-3");
		int selected = menu(terminal, reader, options);

		terminal.writer().println(String.format("\nYou selected: %s", options.get(selected)));
		terminal.writer().flush();
		// JLine3 test : END
	}

	private void drawMenu(Terminal terminal, List<String> options, int selectedOption){
		int pos = 0;

		terminal.writer().print(ansi().eraseScreen().reset());
		terminal.writer().flush();
		//System.out.println(ansi().eraseScreen().reset());
		for (String opt : options){
			if(pos == selectedOption){
				terminal.writer().println(ansi().bg(255).fg(0).a(opt).reset());
				//System.out.println(ansi().bg(255).fg(0).a(opt).reset());
			}else {
				terminal.writer().println(ansi().bg(0).fg(255).a(opt).reset());
				//System.out.println(ansi().bg(0).fg(255).a(opt).reset());
			}
			pos++;
		}
		terminal.writer().flush();
	}

	private int menu(Terminal terminal, LineReader reader, List<String> options){
		int selection = 0;
		// draw initial menu
		drawMenu(terminal, options, selection);

		// loop:
		while (true) {
			int c = ((LineReaderImpl) reader).readCharacter();
			if(c == 119){ // up
				if(selection == 0){
					continue;
				}
				selection--;
				drawMenu(terminal, options, selection);
				continue;
			}else if(c == 115){ // down
				if(selection == options.size()-1){
					continue;
				}
				selection++;
				drawMenu(terminal, options, selection);
				continue;
			}
			if (c == 10 || c == 13) return selection;
		}
			// read key
			// if needed, redraw updated menu
			// if needed return selection
	}

	@SneakyThrows
	protected Void runWithUnirest(UnirestInstance unirest) {
		jline3Test();
		return null;
	}
	
	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return SSCOutputHelper.defaultTableOutputConfig()
				.defaultColumns("id#project.name:Application#name");
	}
}
