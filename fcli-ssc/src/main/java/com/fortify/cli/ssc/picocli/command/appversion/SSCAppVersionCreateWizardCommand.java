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
import org.fusesource.jansi.Ansi;
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

	private static void jline3Test() throws IOException {
		System.setProperty("org.jline.terminal.dumb", "true");

		// JANSI to enable ANSI in Window's CMD
		AnsiConsole.systemInstall();

		// JLine3 stuff
		Terminal terminal = TerminalBuilder.terminal();
		terminal.enterRawMode();

		LineReader reader = LineReaderBuilder.builder()
				.terminal(terminal)
				.build();

		ArrayList<String> options = new ArrayList<>();
		options.add("App1 : Ver1");
		options.add("App1 : Ver2");
		options.add("Pizza App : v1.3.23");

		String msg = "Please select an Application Version:";
		int selected = menu(terminal, reader, options, msg);

		terminal.writer().println(String.format("\nYou selected: %s", options.get(selected)));
		terminal.writer().flush();
	}

	private static void drawMenu(Terminal terminal, List<String> options, int selectedOption, String message){
		String instructions = "(Use \"w\" and \"s\" to move up/down and the \"enter\" key to select.)\n";
		int pos = 0;
		terminal.writer().print(ansi().eraseScreen().cursor(0,0).reset());
		terminal.writer().flush();

		terminal.writer().println(ansi().fg(Ansi.Color.GREEN).a(instructions).fg(Ansi.Color.RED).a(message).reset());
		for (String opt : options){
			if(pos == selectedOption){
				terminal.writer().println(ansi().bg(255).fg(0).a(opt).reset());
			}else {
				terminal.writer().println(ansi().bg(0).fg(255).a(opt).reset());
			}
			pos++;
		}
		terminal.writer().flush();
	}

	// Use this to just update the two lines in the menu. This looks MUCH better than re-drawing the entire menu.
	private static void updateMenu(Terminal terminal, List<String> options, int lineOffset, int currentSelection, int newSelection){
		// probably at the very top or bottom of the menu
		if(currentSelection == newSelection){
			return;
		}

		// unselect old option
		terminal.writer().print(
				ansi()
						.cursor(lineOffset + currentSelection,0)
						.eraseLine()
						.bg(Ansi.Color.BLACK)
						.fg(Ansi.Color.WHITE)
						.a(options.get(currentSelection))
						.cursorToColumn(0)
						.reset()
		);

		// select new option
		terminal.writer().print(
				ansi()
						.cursor(lineOffset + newSelection,0)
						.eraseLine()
						.bg(Ansi.Color.WHITE)
						.fg(Ansi.Color.BLACK)
						.a(options.get(newSelection))
						.cursorToColumn(0)
						.reset()
		);
		terminal.writer().flush();
	}

	private static int menu(Terminal terminal, LineReader reader, List<String> options, String message){
		int selection = 0;
		drawMenu(terminal, options, selection, message);
		int startingOptionOffset = 3;

		while (true) {
			int c = ((LineReaderImpl) reader).readCharacter();
			if(c == 119){ // "w" key is "up"
				if(selection == 0){
					continue;
				}
				selection--;
				updateMenu(terminal, options, startingOptionOffset, selection + 1, selection);
				continue;
			}else if(c == 115){ // "s" key is "down"
				if(selection == options.size()-1){
					continue;
				}
				selection++;
				updateMenu(terminal, options, startingOptionOffset, selection - 1, selection);
				continue;
			}
			if (c == 10 || c == 13) return selection;
		}
	}

	public static void nap() throws InterruptedException {
		Thread.sleep(500);
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
