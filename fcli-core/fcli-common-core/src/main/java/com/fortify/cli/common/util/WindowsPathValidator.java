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
package com.fortify.cli.common.util;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com.fortify.cli.common.exception.FcliSimpleException;

/** Validates path values that may have been corrupted by Windows command-line decoding. */
public final class WindowsPathValidator {
    private static final char REPLACEMENT_CHARACTER = '\uFFFD';

    private WindowsPathValidator() {}

    public static boolean hasUnsupportedCharacters(String value) {
        return PlatformHelper.isWindows()
                && value != null
                && (value.indexOf('?') >= 0 || value.indexOf(REPLACEMENT_CHARACTER) >= 0);
    }

    public static void validate(String optionName, String value) {
        FcliSimpleException.throwIf(
                hasUnsupportedCharacters(value),
                "%s contains '?' or the Unicode replacement character (U+FFFD). "
                        + "This may indicate corruption during Windows command-line decoding; "
                        + "please rerun the command with a valid Windows path",
                optionName);
    }

    public static Path toPath(String optionName, String value) {
        validate(optionName, value);
        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            throw new FcliSimpleException(
                    "%s is not a valid file path on this platform; please provide a supported file path",
                    optionName);
        }
    }
}
