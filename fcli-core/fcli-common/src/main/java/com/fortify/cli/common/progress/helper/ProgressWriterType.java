/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.progress.helper;

import java.io.PrintStream;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.util.ConsoleHelper;

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
        private static final Logger LOG = LoggerFactory.getLogger(AbstractProgressWriter.class);
        protected final PrintStream progressOut;
        protected final PrintStream progressErr;

        protected AbstractProgressWriter() {
            this.progressOut = StdioHelper.getProgressOut();
            this.progressErr = StdioHelper.getProgressErr();
            var currentOut = StdioHelper.currentOut();
            var currentErr = StdioHelper.currentErr();
            var wrappedOut = new ProgressWriterPrintStreamWrapper(currentOut, this);
            var wrappedErr = new ProgressWriterPrintStreamWrapper(currentErr, this);
            StdioHelper.pushOut(wrappedOut);
            StdioHelper.pushErr(wrappedErr);
        }

        @Override
        public void close() {
            StdioHelper.popOut();
            StdioHelper.popErr();
            clearProgress();
        }
        
        @Override
        public final void writeWarning(String message, Object... args) {
            var formattedMessage = format(message, args);
            LOG.debug("writeWarning: {}", formattedMessage);
            System.err.println(formattedMessage);
        }
        
        @Override
        public final void writeWarningWithException(String message, Throwable cause, Object... args) {
            var formattedMessage = format(message, args);
            LOG.debug("writeWarningWithException: {}", formattedMessage, cause);
            System.err.println(formattedMessage);
            if (cause != null) {
                System.err.println("Cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            }
        }
        
        @Override
        public final void writeProgress(String message, Object... args) {
            var formattedMessage = format(message, args);
            LOG.debug("writeProgress: {}", formattedMessage);
            var callback = StdioHelper.getProgressCallback();
            if (callback != null) {
                callback.accept(formattedMessage);
            }
            writeProgress(formattedMessage);
        }
        
        protected abstract void writeProgress(String message);
        
        @Override
        public final void writeInfo(String message, Object... args) {
            var formattedMessage = format(message, args);
            LOG.debug("writeInfo: {}", formattedMessage);
            System.out.println(formattedMessage);
        }
        
        @Override
        public final void writeInfoWithException(String message, Throwable cause, Object... args) {
            var formattedMessage = format(message, args);
            LOG.debug("writeInfoWithException: {}", formattedMessage, cause);
            System.out.println(formattedMessage);
            if (cause != null) {
                System.out.println("Cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            }
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
                message += "\n";
            }
            progressOut.println(message);
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
                message += "\n";
            }
            progressErr.println(message);
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
            var terminalWidth = ConsoleHelper.getTerminalWidth();
            var abbreviatedMessage = terminalWidth==null ? message : StringUtils.abbreviate(message, terminalWidth);
            progressOut.print(abbreviatedMessage);
            this.lastNumberOfChars = abbreviatedMessage.length();
        }
        @Override
        public void clearProgress() {
            if ( lastNumberOfChars>0 ) {
                progressOut.print(LINE_START+" ".repeat(lastNumberOfChars)+LINE_START);
                lastNumberOfChars = 0;
            }
        }
    }
    // TODO Current implementation disables line wrapping, to be able to clear the proper number of lines.
    //      Ideally, we should allow line wrapping and use save/restore cursor approach as commented out below,
    //      but this doesn't work properly if multiple progress writers are nested. For example, if an fcli
    //      command instantiates a progress writer, but then calls SSCFileTransferHelper which also instantiates
    //      a progress writer, cursor position saved by the outer progress writer may be overwritten by the inner
    //      progress writer. Ideally, only commands should instantiate progress writers; utility classes like
    //      SSCFileTransferHelper should receive a progress writer as parameter instead of instantiating one.
    //      Alternatively, maybe we can have an inner progress writer 'inherit'/use the outer progress writer,
    //      for example by saving outer progress writer in a ThreadLocal variable.
    private static final class AnsiProgressWriter extends AbstractProgressWriter {
        private static final String WRAP_ENABLE = "\033[?7h";
        private static final String WRAP_DISABLE = "\033[?7l";
        private static final String LINE_UP = "\033[1A";
        private static final String LINE_CLEAR = "\033[2K";
        private static final String LINE_START = "\r";
        //private static final String SAVE_CURSOR = "\0337";
        //private static final String RESTORE_CURSOR = "\0338";
        //private static final String ERASE_DOWN = "\033[J";
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
            progressOut.print(WRAP_DISABLE+message+WRAP_ENABLE);
            //progressOut.print(SAVE_CURSOR + message);
            this.lastNumberOfLines = (int)message.chars().filter(ch -> ch == '\n').count()+1;
        }
        @Override
        public void clearProgress() {
            if ( lastNumberOfLines>0 ) {
                progressOut.print((LINE_CLEAR+LINE_UP).repeat(lastNumberOfLines-1)+LINE_CLEAR+LINE_START);
                //progressOut.print(RESTORE_CURSOR + ERASE_DOWN);
                lastNumberOfLines = 0;
            }
        }
    }
}