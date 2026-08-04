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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class WindowsCommandLineArgsTest {

    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final char REPLACEMENT = '\uFFFD';

    @Test
    void isLikelyJnuCorruptionDetectsUnmappablePath() {
        String wide = "C:\\Users\\test\\\u4e2d\u6587\\\u0627\u0644\u0639\u0631\u0628\u064a\\cache.zip";
        String jvm = new String(wide.getBytes(CP1252), CP1252);

        assertTrue(jvm.contains("?"));
        assertTrue(WindowsCommandLineArgs.isLikelyJnuCorruption(jvm, wide, CP1252));
        assertTrue(WindowsCommandLineArgs.isLikelyCorruptedArg(jvm, wide, CP1252));
        assertFalse(WindowsCommandLineArgs.isLikelyJnuCorruption(wide, wide, CP1252));
        assertFalse(WindowsCommandLineArgs.isLikelyJnuCorruption(jvm, "other", CP1252));
    }

    @Test
    void isLikelyReplacementCharCorruptionDetectsNonAsciiLoss() {
        String wide = "C:\\tmp\\\u4e2d\u6587\\\u0440\u0443\\cache.zip";
        String graal = replaceNonAscii(wide);

        assertTrue(graal.indexOf(REPLACEMENT) >= 0);
        assertTrue(WindowsCommandLineArgs.isLikelyReplacementCharCorruption(graal, wide));
        assertTrue(WindowsCommandLineArgs.isLikelyCorruptedArg(graal, wide, CP1252));
        assertFalse(WindowsCommandLineArgs.isLikelyReplacementCharCorruption(wide, wide));
        assertFalse(WindowsCommandLineArgs.isLikelyReplacementCharCorruption("C:\\tmp\\??\\cache.zip", wide));
    }

    @Test
    void mergeCorruptedArgsRestoresJnuCorruptedTokens() {
        String widePath = "C:\\tmp\\\u65e5\u672c\u8a9e\\cache.zip";
        String jvmPath = new String(widePath.getBytes(CP1252), CP1252);
        String[] jvm = {"aviator", "ssc", "apply-remediations", "--from-cache", jvmPath};
        String[] wide = {"aviator", "ssc", "apply-remediations", "--from-cache", widePath};

        String[] fixed = WindowsCommandLineArgs.mergeCorruptedArgs(jvm, wide, CP1252);

        assertEquals(widePath, fixed[4]);
        assertEquals("aviator", fixed[0]);
        assertEquals("--from-cache", fixed[3]);
    }

    @Test
    void mergeCorruptedArgsRestoresReplacementCharTokens() {
        String widePath = "C:\\tmp\\\u4e2d\u6587\\cache.zip";
        String[] jvm = {"aviator", "--from-cache", replaceNonAscii(widePath)};
        String[] wide = {"aviator", "--from-cache", widePath};

        String[] fixed = WindowsCommandLineArgs.mergeCorruptedArgs(jvm, wide, CP1252);

        assertEquals(widePath, fixed[2]);
        assertEquals("aviator", fixed[0]);
    }

    @Test
    void mergeCorruptedArgsKeepsJvmArgsWhenWideIsUnrelated() {
        String[] jvm = {"aviator", "--from-cache", "C:\\plain\\cache.zip"};
        String[] wide = {"org.gradle.worker.internal.WorkerProcess", "something", "else"};

        String[] fixed = WindowsCommandLineArgs.mergeCorruptedArgs(jvm, wide, CP1252);

        assertArrayEquals(jvm, fixed);
    }

    @Test
    void fixIfNeededReturnsSameArrayWhenEmpty() {
        String[] empty = new String[0];
        assertSame(empty, WindowsCommandLineArgs.fixIfNeeded(empty));
        assertSame(null, WindowsCommandLineArgs.fixIfNeeded(null));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void fixIfNeededDoesNotThrowOnWindows() {
        String[] args = {"aviator", "ssc", "--help"};
        String[] fixed = WindowsCommandLineArgs.fixIfNeeded(args);
        assertEquals(args.length, fixed.length);
        assertArrayEquals(args, fixed);
    }

    private static String replaceNonAscii(String wide) {
        StringBuilder sb = new StringBuilder(wide.length());
        for (int i = 0; i < wide.length(); i++) {
            char c = wide.charAt(i);
            sb.append(c <= 0x7F ? c : REPLACEMENT);
        }
        return sb.toString();
    }
}
