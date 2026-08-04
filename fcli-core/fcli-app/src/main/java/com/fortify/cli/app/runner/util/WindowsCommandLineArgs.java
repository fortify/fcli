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
package com.fortify.cli.app.runner.util;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Restores Windows command-line arguments that were corrupted by lossy
 * encoding of characters outside the active ANSI code page.
 *
 * <p>On Windows, {@code java -jar} (and some Graal native images) can replace
 * non-ANSI code points in argv with {@code '?'} or U+FFFD before
 * {@code main} runs. That breaks path options such as {@code --from-cache},
 * {@code -f}, and {@code --source-dir} for any script not covered by the
 * process code page (CJK, Arabic, Cyrillic, emoji, etc.).</p>
 *
 * <p>This helper re-reads the process command line via
 * {@code GetCommandLineW} / {@code CommandLineToArgvW} and replaces only
 * tokens that look like a lossy form of the wide originals. It is a no-op on
 * non-Windows, when re-parse fails or token counts differ, and never throws.
 * On systems that already deliver correct argv (including UTF-8
 * {@code sun.jnu.encoding}), merge is a no-op because tokens match the wide
 * form.</p>
 */
public final class WindowsCommandLineArgs {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsCommandLineArgs.class);
    private static final char REPLACEMENT = '\uFFFD';

    private WindowsCommandLineArgs() {}

    /**
     * Return {@code args} with Unicode restored where a wide re-parse shows
     * lossy corruption; otherwise return {@code args} unchanged.
     */
    public static String[] fixIfNeeded(String[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        if (!isWindows()) {
            return args;
        }
        try {
            String[] wideAppArgs = readWideApplicationArgs(args.length);
            if (wideAppArgs == null || wideAppArgs.length != args.length) {
                return args;
            }
            return mergeCorruptedArgs(args, wideAppArgs, jnuCharset());
        } catch (Throwable t) {
            LOG.debug("Windows wide argv recovery skipped: {}", t.toString());
            return args;
        }
    }

    static String[] mergeCorruptedArgs(String[] jvmArgs, String[] wideArgs, Charset jnu) {
        String[] result = Arrays.copyOf(jvmArgs, jvmArgs.length);
        boolean changed = false;
        for (int i = 0; i < jvmArgs.length; i++) {
            if (isLikelyCorruptedArg(jvmArgs[i], wideArgs[i], jnu)) {
                result[i] = wideArgs[i];
                changed = true;
            }
        }
        if (changed) {
            LOG.debug("Restored Unicode command-line argument(s) via Windows wide API");
        }
        return result;
    }

    static boolean isLikelyCorruptedArg(String jvmArg, String wideArg, Charset jnu) {
        if (jvmArg == null || wideArg == null || jvmArg.equals(wideArg)) {
            return false;
        }
        return isLikelyJnuCorruption(jvmArg, wideArg, jnu)
                || isLikelyReplacementCharCorruption(jvmArg, wideArg);
    }

    static boolean isLikelyJnuCorruption(String jvmArg, String wideArg, Charset jnu) {
        if (jvmArg == null || wideArg == null || jvmArg.equals(wideArg)) {
            return false;
        }
        String roundTrip = new String(wideArg.getBytes(jnu), jnu);
        return jvmArg.equals(roundTrip);
    }

    static boolean isLikelyReplacementCharCorruption(String jvmArg, String wideArg) {
        if (jvmArg == null || wideArg == null || jvmArg.length() != wideArg.length()) {
            return false;
        }
        if (jvmArg.indexOf(REPLACEMENT) < 0) {
            return false;
        }
        for (int i = 0; i < wideArg.length(); i++) {
            char w = wideArg.charAt(i);
            char j = jvmArg.charAt(i);
            char expected = w <= 0x7F ? w : REPLACEMENT;
            if (j != expected) {
                return false;
            }
        }
        return true;
    }

    static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(Locale.ROOT).contains("win");
    }

    static Charset jnuCharset() {
        String name = System.getProperty("sun.jnu.encoding");
        if (name == null || name.isBlank()) {
            return Charset.defaultCharset();
        }
        try {
            return Charset.forName(name);
        } catch (Exception e) {
            return Charset.defaultCharset();
        }
    }

    private static String[] readWideApplicationArgs(int appArgCount) {
        if (appArgCount <= 0) {
            return new String[0];
        }
        IntByReference argc = new IntByReference();
        WString cmdLine = Kernel32.INSTANCE.GetCommandLineW();
        if (cmdLine == null) {
            return null;
        }
        Pointer argv = Shell32.INSTANCE.CommandLineToArgvW(cmdLine, argc);
        if (argv == null) {
            return null;
        }
        try {
            int n = argc.getValue();
            if (n < appArgCount) {
                return null;
            }
            String[] all = argv.getWideStringArray(0, n);
            return Arrays.copyOfRange(all, all.length - appArgCount, all.length);
        } finally {
            Kernel32.INSTANCE.LocalFree(argv);
        }
    }

    private interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        WString GetCommandLineW();

        Pointer LocalFree(Pointer hMem);
    }

    private interface Shell32 extends Library {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer CommandLineToArgvW(WString lpCmdLine, IntByReference pNumArgs);
    }
}
