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
package com.fortify.cli.common.http.ssl.trust;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class FcliTrustManagerTest {
    @Test
    void skipsMacKeychainInNativeRuntime() throws Exception {
        var previousOs = System.getProperty("os.name");
        var previousImageCode = System.getProperty("org.graalvm.nativeimage.imagecode");
        var logger = (Logger) LoggerFactory.getLogger(FcliTrustManager.class);
        var previousLevel = logger.getLevel();
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            System.setProperty("os.name", "Mac OS X");
            System.setProperty("org.graalvm.nativeimage.imagecode", "runtime");
            var method = FcliTrustManager.class.getDeclaredMethod("loadPlatformKeyStore");
            method.setAccessible(true);
            assertNull(method.invoke(FcliTrustManager.getInstance()));
            assertTrue(appender.list.stream().anyMatch(event -> event.getLevel() == Level.DEBUG
                    && event.getFormattedMessage().contains("macOS Keychain trust store is not supported in native images")));
            assertTrue(appender.list.stream().noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN)));
        } finally {
            restoreProperty("os.name", previousOs);
            restoreProperty("org.graalvm.nativeimage.imagecode", previousImageCode);
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
