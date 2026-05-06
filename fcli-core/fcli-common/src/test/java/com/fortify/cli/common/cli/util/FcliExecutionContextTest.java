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
package com.fortify.cli.common.cli.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.session.helper.ISessionDescriptor;

class FcliExecutionContextTest {
    @Test
    void transientSessionDescriptorsCanBeStoredByTypeAndCleared() {
        var context = new FcliExecutionContext();
        var sscDescriptor = new DummySessionDescriptor("SSC");
        var fodDescriptor = new DummySessionDescriptor("FoD");

        assertTrue(context.getTransientSessionDescriptors().isEmpty());
        assertNull(context.getTransientSessionDescriptor("SSC"));
        assertFalse(context.info().contains("transientSessions=1"));

        context.setTransientSessionDescriptor(sscDescriptor);
        context.setTransientSessionDescriptor(fodDescriptor);

        assertSame(sscDescriptor, context.getTransientSessionDescriptor("SSC"));
        assertSame(fodDescriptor, context.getTransientSessionDescriptor("FoD"));
        assertTrue(context.info().contains("transientSessions=2"));

        context.clearTransientSessionDescriptor("SSC");

        assertNull(context.getTransientSessionDescriptor("SSC"));
        assertSame(fodDescriptor, context.getTransientSessionDescriptor("FoD"));

        context.clearTransientSessionDescriptors();

        assertTrue(context.getTransientSessionDescriptors().isEmpty());
    }

    @Test
    void transientSessionDescriptorConvenienceSetterIndexesByType() {
        var context = new FcliExecutionContext();
        var descriptor = new DummySessionDescriptor("dummy");

        context.setTransientSessionDescriptor(descriptor);

        assertSame(descriptor, context.getTransientSessionDescriptor("dummy"));
    }

    private static final class DummySessionDescriptor implements ISessionDescriptor {
        private final String type;

        private DummySessionDescriptor(String type) {
            this.type = type;
        }

        @Override
        public String getUrlDescriptor() {
            return "dummy";
        }

        @Override
        public Date getCreatedDate() {
            return new Date();
        }

        @Override
        public Date getExpiryDate() {
            return null;
        }

        @Override
        public String getType() {
            return type;
        }
    }
}