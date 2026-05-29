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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.session.helper.ISessionDescriptor;

class FcliExecutionContextTest {
    @Test
    void transientSessionDescriptorsCanBeStoredByTypeAndCleared() {
        try (var context = new FcliExecutionContext()) {
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
    }

    @Test
    void transientSessionDescriptorConvenienceSetterIndexesByType() {
        try (var context = new FcliExecutionContext()) {
            var descriptor = new DummySessionDescriptor("dummy");

            context.getIsolationScope().setTransientSessionDescriptor(descriptor);

            assertSame(descriptor, context.getIsolationScope().getTransientSessionDescriptor("dummy"));
        }
    }

    @Test
    void pushNewAlwaysCreatesAFreshContext() {
        FcliExecutionContextHolder.pushNew();
        try {
            var parent = FcliExecutionContextHolder.current();
            parent.getIsolationScope().setMcpRequestAuthScopeKey("ssc|abc123");
            FcliExecutionContextHolder.pushNew();
            try {
                var child = FcliExecutionContextHolder.current();
                // pushNew always creates a completely new context, so isolation scope is NOT inherited
                var childScopeKey = child.getIsolationScope().getMcpRequestAuthScopeKey();
                assertTrue(childScopeKey == null || childScopeKey.isEmpty());
                assertFalse(parent.getIsolationScope() == child.getIsolationScope());
                assertTrue(child.getActionState().getGlobalActionValues().isEmpty());
            } finally {
                FcliExecutionContextHolder.pop();
            }
        } finally {
            FcliExecutionContextHolder.pop();
        }
    }

    @Test
    void createChildInheritsIsolationScopeAndCreatesFreshActionState() {
        try (var parent = new FcliExecutionContext(); var child = parent.createChild()) {
            assertSame(parent.getIsolationScope(), child.getIsolationScope());
            assertTrue(child.getActionState().getGlobalActionValues().isEmpty());
        }
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