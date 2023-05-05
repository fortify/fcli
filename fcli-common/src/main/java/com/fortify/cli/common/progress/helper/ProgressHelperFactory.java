package com.fortify.cli.common.progress.helper;

import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine.Help.Ansi;

public final class ProgressHelperFactory {
    private static final boolean hasConsole = System.console()!=null;
    private static final boolean hasAnsiConsole = Ansi.AUTO.enabled() && hasConsole;
    private static final String LINE_UP = "\033[1A";
    private static final String LINE_CLEAR = "\033[2K";
    private static final String LINE_START = "\r";
    
    private ProgressHelperFactory() {}
    
    public static final IProgressHelper createProgressHelper(boolean noProgress) {
        if ( noProgress ) { return new DummyProgressHelper(); }
        else if ( hasAnsiConsole ) { return new AnsiConsoleProgressHelper(); }
        else if ( hasConsole ) { return new BasicConsoleProgressHelper(); }
        else { return new BasicProgressHelper(); }
    }
    
    private static abstract class AbstractProgressHelper implements IProgressHelper {
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
    
    private static final class DummyProgressHelper extends AbstractProgressHelper {
        @Override
        public boolean isMultiLineSupported() {
            return false;
        }
        
        @Override
        public void writeProgress(String message, Object... args) {}
        
        @Override
        public void clearProgress() {}
    }
    
    private static final class BasicProgressHelper extends AbstractProgressHelper {
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
    
    private static final class BasicConsoleProgressHelper extends AbstractProgressHelper {
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
    
    private static final class AnsiConsoleProgressHelper extends AbstractProgressHelper {
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
