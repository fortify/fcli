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
package com.fortify.cli.fod._common.session.helper.sso;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.fortify.cli.common.exception.FcliSimpleException;

/**
 * Locates a Chromium-based browser (Chrome or Edge) that supports the DevTools protocol.
 */
final class FoDSsoBrowserLocator {
    private FoDSsoBrowserLocator() {}

    /**
     * @param browserType Browser to use ("chrome" or "edge"), or blank to use the first browser found (Chrome, then Edge)
     * @param explicitPath Browser executable; takes precedence over browserType
     */
    static Path locate(String browserType, String explicitPath) {
        if ( StringUtils.isNotBlank(explicitPath) ) {
            try {
                var path = Path.of(explicitPath.trim());
                FcliSimpleException.throwIf(!Files.isExecutable(path), "Browser not found or not executable: %s", explicitPath);
                return path;
            } catch ( InvalidPathException e ) {
                throw new FcliSimpleException("Invalid --browser-path: " + explicitPath);
            }
        }
        var chrome = isBrowser(browserType, "chrome");
        var edge = isBrowser(browserType, "edge");
        var name = chrome ? "Google Chrome" : edge ? "Microsoft Edge" : "Google Chrome or Microsoft Edge";
        return candidates(SystemUtils.IS_OS_WINDOWS, SystemUtils.IS_OS_MAC, System::getenv, chrome, edge).stream()
                .filter(Files::isExecutable)
                .findFirst()
                .orElseThrow(() -> new FcliSimpleException("%s not found. Specify the browser executable using "
                        + "--browser-path, or use --saml-assertion-file instead.", name));
    }

    private static boolean isBrowser(String browserType, String expected) {
        if ( StringUtils.isBlank(browserType) ) { return true; }
        var type = browserType.trim().toLowerCase();
        FcliSimpleException.throwIf(!type.equals("chrome") && !type.equals("edge"),
                "Invalid --browser value '%s'; supported values: chrome, edge", browserType);
        return type.equals(expected);
    }

    static List<Path> candidates(boolean windows, boolean mac, Function<String, String> env) {
        return candidates(windows, mac, env, true, true);
    }

    static List<Path> candidates(boolean windows, boolean mac, Function<String, String> env, boolean chrome, boolean edge) {
        if ( windows ) { return windowsCandidates(env, chrome, edge); }
        if ( mac ) { return macCandidates(chrome, edge); }
        return linuxCandidates(env, chrome, edge);
    }

    private static List<Path> windowsCandidates(Function<String, String> env, boolean chrome, boolean edge) {
        var result = new ArrayList<Path>();
        if ( chrome ) {
            for ( var base : new String[] {"ProgramFiles", "ProgramFiles(x86)", "LOCALAPPDATA"} ) {
                add(result, env.apply(base), "Google", "Chrome", "Application", "chrome.exe");
            }
        }
        if ( edge ) {
            for ( var base : new String[] {"ProgramFiles(x86)", "ProgramFiles", "LOCALAPPDATA"} ) {
                add(result, env.apply(base), "Microsoft", "Edge", "Application", "msedge.exe");
            }
        }
        return result;
    }

    private static List<Path> macCandidates(boolean chrome, boolean edge) {
        var result = new ArrayList<Path>();
        if ( chrome ) { result.add(Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")); }
        if ( edge ) { result.add(Path.of("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge")); }
        if ( chrome && edge ) { result.add(Path.of("/Applications/Chromium.app/Contents/MacOS/Chromium")); }
        return result;
    }

    private static List<Path> linuxCandidates(Function<String, String> env, boolean chrome, boolean edge) {
        var result = new ArrayList<Path>();
        var pathDirs = StringUtils.defaultString(env.apply("PATH")).split(File.pathSeparator);
        var names = new ArrayList<String>();
        if ( chrome ) { names.addAll(List.of("google-chrome", "google-chrome-stable")); }
        if ( chrome && edge ) { names.addAll(List.of("chromium", "chromium-browser")); }
        if ( edge ) { names.addAll(List.of("microsoft-edge", "microsoft-edge-stable")); }
        for ( var name : names ) {
            for ( var dir : pathDirs ) { add(result, dir, name); }
        }
        return result;
    }

    private static void add(List<Path> target, String base, String... parts) {
        if ( StringUtils.isBlank(base) ) { return; }
        try {
            target.add(Path.of(base, parts));
        } catch ( InvalidPathException ignore ) {}
    }
}
