/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.progress.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Help.Ansi;

@RequiredArgsConstructor
public enum ProgressWriterType {
    auto(ProgressWriterType::auto), 
    none(NoProgressWriter::new), 
    simple(SimpleProgressWriter::new),
    stderr(SimpleStdErrProgressWriter::new),
    single_line(SingleLineProgressWriter::new), 
    ansi(AnsiProgressWriter::new);
    
    private final Supplier<IProgressWriter> factory;
    
    public IProgressWriter create() {
        return factory.get();
    }
    
    private static final IProgressWriter auto() {
        var hasConsole = System.console()!=null;
        var hasAnsiConsole = Ansi.AUTO.enabled() && hasConsole;
        if ( hasAnsiConsole ) { return new AnsiProgressWriter(); }
        else if ( hasConsole ) { return new SingleLineProgressWriter(); }
        else { return new NoProgressWriter(); }
    }
    
    private static abstract class AbstractProgressWriter implements IProgressWriter {
        private final List<String> warnings = new ArrayList<>();
        
        @Override
        public void writeWarning(String message, Object... args) {
            warnings.add(String.format(message, args));
        }
        
        @Override
        public void close() {
            clearProgress();
            warnings.forEach(System.err::println);
        }
    }
    
    private static final class NoProgressWriter extends AbstractProgressWriter {
        @Override
        public boolean isMultiLineSupported() {
            return false;
        }
        
        @Override
        public void writeProgress(String message, Object... args) {}
        
        @Override
        public void clearProgress() {}
    }
    
    private static final class SimpleProgressWriter extends AbstractProgressWriter {
        @Override
        public boolean isMultiLineSupported() {
            return true;
        }
        
        @Override
        public void writeProgress(String message, Object... args) {
            String formattedMessage = String.format(message, args);
            if ( formattedMessage.indexOf('\n') > 0 ) {
                // Add extra newline to separate multi-line blocks
                formattedMessage += "\n";
            }
            System.out.println(formattedMessage);
        }
        
        @Override
        public void clearProgress() {}
    }
    
    private static final class SimpleStdErrProgressWriter extends AbstractProgressWriter {
        @Override
        public boolean isMultiLineSupported() {
            return true;
        }
        
        @Override
        public void writeProgress(String message, Object... args) {
            String formattedMessage = String.format(message, args);
            if ( formattedMessage.indexOf('\n') > 0 ) {
                // Add extra newline to separate multi-line blocks
                formattedMessage += "\n";
            }
            System.err.println(formattedMessage);
        }
        
        @Override
        public void clearProgress() {}
    }
    
    private static final class SingleLineProgressWriter extends AbstractProgressWriter {
        private static final String LINE_START = "\r";
        private int lastNumberOfChars;
        
        @Override
        public boolean isMultiLineSupported() {
            return false;
        }
        
        @Override
        public void writeProgress(String message, Object... args) {
            if ( message.contains("\n") ) { throw new RuntimeException("Multiline status updates are not supported; please file a bug"); }
            clearProgress();
            String formattedMessage = String.format(message, args);
            System.out.print(formattedMessage);
            this.lastNumberOfChars = formattedMessage.length();
        }
        
        @Override
        public void clearProgress() {
            System.out.print(LINE_START+" ".repeat(lastNumberOfChars)+LINE_START);
        }
    }
    
    private static final class AnsiProgressWriter extends AbstractProgressWriter {
        // TODO Can we improve this to simply clear all output written so far?
        private static final String LINE_UP = "\033[1A";
        private static final String LINE_CLEAR = "\033[2K";
        private static final String LINE_START = "\r";
        private int lastNumberOfLines = 0;
        
        @Override
        public boolean isMultiLineSupported() {
            return true;
        }
        
        @Override
        public void writeProgress(String message, Object... args) {
            clearProgress();
            String formattedMessage = String.format(message, args);
            System.out.print(formattedMessage);
            this.lastNumberOfLines = (int)formattedMessage.chars().filter(ch -> ch == '\n').count();
        }
        
        @Override
        public void clearProgress() {
            // TODO Any way we can use ESC[3J to clear all saved lines, instead of removing lines one-by-one?
            //      Not sure what escape code to use for 'start lines to be saved'...
            System.out.print((LINE_CLEAR+LINE_UP).repeat(lastNumberOfLines)+LINE_CLEAR+LINE_START);
            lastNumberOfLines = 0;
        }
    }
}
