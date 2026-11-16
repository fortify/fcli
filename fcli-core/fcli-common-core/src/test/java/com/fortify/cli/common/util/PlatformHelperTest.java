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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlatformHelperTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"runtime", "buildtime", "unknown"})
    void detectsNativeRuntimeOnly(String imageCode) {
        var propertyName = "org.graalvm.nativeimage.imagecode";
        var previous = System.getProperty(propertyName);
        try {
            if (imageCode == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, imageCode);
            }
            assertEquals("runtime".equals(imageCode), PlatformHelper.isNativeImage());
        } finally {
            if (previous == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, previous);
            }
        }
    }
}
