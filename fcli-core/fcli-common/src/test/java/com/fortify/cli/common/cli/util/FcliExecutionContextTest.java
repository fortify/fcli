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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        assertTrue(context.getIsolationScope().getTransientSessionDescriptors().isEmpty());
        assertNull(context.getIsolationScope().getTransientSessionDescriptor("SSC"));
        assertFalse(context.info().contains("transientSessions=1"));

        context.getIsolationScope().setTransientSessionDescriptor(sscDescriptor);
        context.getIsolationScope().setTransientSessionDescriptor(fodDescriptor);

        assertSame(sscDescriptor, context.getIsolationScope().getTransientSessionDescriptor("SSC"));
        assertSame(fodDescriptor, context.getIsolationScope().getTransientSessionDescriptor("FoD"));
        assertTrue(context.info().contains("transientSessions=2"));

        context.getIsolationScope().clearTransientSessionDescriptor("SSC");

        assertNull(context.getIsolationScope().getTransientSessionDescriptor("SSC"));
        assertSame(fodDescriptor, context.getIsolationScope().getTransientSessionDescriptor("FoD"));

        context.getIsolationScope().clearTransientSessionDescriptors();

        assertTrue(context.getIsolationScope().getTransientSessionDescriptors().isEmpty());
    }

    @Test
    void transientSessionDescriptorConvenienceSetterIndexesByType() {
        var context = new FcliExecutionContext();
        var descriptor = new DummySessionDescriptor("dummy");

        context.getIsolationScope().setTransientSessionDescriptor(descriptor);

        assertSame(descriptor, context.getIsolationScope().getTransientSessionDescriptor("dummy"));
    }

    @Test
    void pushNewInheritsIsolationScopeButCreatesFreshActionState() {
        FcliExecutionContextHolder.pushNew();
        try {
            var parent = FcliExecutionContextHolder.current();
            parent.getIsolationScope().setMcpRequestAuthScopeKey("ssc|abc123");
            FcliExecutionContextHolder.pushNew();
            try {
                var child = FcliExecutionContextHolder.current();
                assertEquals("ssc|abc123", FcliExecutionContextHolder.getMcpRequestAuthScopeKey());
                assertSame(parent.getIsolationScope(), child.getIsolationScope());
                assertTrue(child.getActionState().getGlobalActionValues().isEmpty());
            } finally {
                FcliExecutionContextHolder.pop();
            }
        } finally {
            FcliExecutionContextHolder.pop();
        }
    }

    @Test
    void childContextsCanChooseFreshOrSharedActionState() {
        var parent = new FcliExecutionContext();
        var freshChild = parent.createChild();
        var sharedChild = parent.createChildWithSharedActionState();

        assertSame(parent.getIsolationScope(), freshChild.getIsolationScope());
        assertSame(parent.getIsolationScope(), sharedChild.getIsolationScope());
        assertTrue(freshChild.getActionState().getGlobalActionValues().isEmpty());
        assertSame(parent.getActionState(), sharedChild.getActionState());
    }

    @Test
    void currentThrowsWhenNoContextHasBeenPushed() {
        // Verify that current() never silently creates a context — callers must push explicitly.
        assertThrows(IllegalStateException.class, FcliExecutionContextHolder::current);
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