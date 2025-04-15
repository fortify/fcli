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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliBugException;

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
    
    private static final Logger LOG = LoggerFactory.getLogger(ProgressWriterType.class);
    
    @Override
    public String toString() {
        // Show and accept dashes instead of underscores when this
        // enum is used in picocli options.
        return name().replace('_', '-');
    }
    
    private final Supplier<IProgressWriter> factory;
    
    public IProgressWriter create() {
        return factory.get();
    }
    
    private static final IProgressWriter auto() {
        var hasConsole = System.console()!=null;
        var hasAnsiConsole = Ansi.AUTO.enabled() && hasConsole;
        if ( hasAnsiConsole ) { return new AnsiProgressWriter(); }
        else if ( hasConsole ) { return new SingleLineProgressWriter(); }
        else { return new SimpleProgressWriter(); }
    }
    
    private static abstract class AbstractProgressWriter implements IProgressWriter {
        // Store original stdout/stderr, as these may be suppressed/delayed during fcli action execution 
        protected final PrintStream stdout = System.out;
        protected final PrintStream stderr = System.err;
        private final List<String> warnings = new ArrayList<>();
        private final List<String> info = new ArrayList<>();
        
        @Override
        public final void writeWarning(String message, Object... args) {
            var msg = format(message, args);
            LOG.warn(msg);
            writeWarning(msg);
        }

        protected void writeWarning(String message) {
            warnings.add(message);
        }
        
        @Override
        public final void writeProgress(String message, Object... args) {
            var msg = format(message, args);
            LOG.info(msg);
            writeProgress(msg);
        }
        
        protected abstract void writeProgress(String message);
        
        @Override
        public final void writeInfo(String message, Object... args) {
            var msg = format(message, args);
            LOG.info(msg);
            writeInfo(msg);
        }

        protected void writeInfo(String message) {
            info.add(message);
        }
        
        @Override
        public void close() {
            clearProgress();
            warnings.forEach(stderr::println);
            info.forEach(stdout::println);
        }
        
        private final String format(String message, Object... args) {
            if ( args==null || args.length==0 ) {
                return message;
            } else {
                return String.format(message, args);
            }
        }
    }
    
    private static final class NoProgressWriter extends AbstractProgressWriter {
        @Override
        public String type() {
            return "none";
        }
        
        @Override
        public boolean isMultiLineSupported() {
            return false;
        }
        
        @Override
        public void writeProgress(String message) {}
        
        @Override
        public void clearProgress() {}
    }
    
    private static final class SimpleProgressWriter extends AbstractProgressWriter {
        @Override
        public String type() {
            return "simple";
        }
        
        @Override
        public boolean isMultiLineSupported() {
            return true;
        }
        
        @Override
        public void writeProgress(String message) {
            if ( message.indexOf('\n') > 0 ) {
                // Add extra newline to separate multi-line blocks
                message += "\n";
            }
            stdout.println(message);
        }
        
        @Override
        public void clearProgress() {}
    }
    
    private static final class SimpleStdErrProgressWriter extends AbstractProgressWriter {
        @Override
        public String type() {
            return "stderr";
        }
        
        @Override
        public boolean isMultiLineSupported() {
            return true;
        }
        
        @Override
        public void writeProgress(String message) {
            if ( message.indexOf('\n') > 0 ) {
                // Add extra newline to separate multi-line blocks
                message += "\n";
            }
            stderr.println(message);
        }
        
        @Override
        public void clearProgress() {}
    }
    
    private static final class SingleLineProgressWriter extends AbstractProgressWriter {
        private static final String LINE_START = "\r";
        private int lastNumberOfChars;
        
        @Override
        public String type() {
            return "single-line";
        }
        
        @Override
        public boolean isMultiLineSupported() {
            return false;
        }
        
        @Override
        public void writeProgress(String message) {
            if ( message.contains("\n") ) { throw new FcliBugException("Multiline status updates are not supported; please file a bug"); }
            clearProgress();
            stdout.print(message);
            this.lastNumberOfChars = message.length();
        }
        
        @Override
        public void clearProgress() {
            stdout.print(LINE_START+" ".repeat(lastNumberOfChars)+LINE_START);
        }
    }
    
    private static final class AnsiProgressWriter extends AbstractProgressWriter {
        // TODO Can we improve this to simply clear all output written so far?
        private static final String LINE_UP = "\033[1A";
        private static final String LINE_CLEAR = "\033[2K";
        private static final String LINE_START = "\r";
        private int lastNumberOfLines = 0;
        
        @Override
        public String type() {
            return "ansi";
        }
        
        @Override
        public boolean isMultiLineSupported() {
            return true;
        }
        
        @Override
        public void writeProgress(String message) {
            clearProgress();
            stdout.print(message);
            this.lastNumberOfLines = (int)message.chars().filter(ch -> ch == '\n').count();
        }
        
        @Override
        public void clearProgress() {
            // TODO Any way we can use ESC[3J to clear all saved lines, instead of removing lines one-by-one?
            //      Not sure what escape code to use for 'start lines to be saved'...
            stdout.print((LINE_CLEAR+LINE_UP).repeat(lastNumberOfLines)+LINE_CLEAR+LINE_START);
            lastNumberOfLines = 0;
        }
    }
}
