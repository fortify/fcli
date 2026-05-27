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
package com.fortify.cli.common.session.cli.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

class AbstractSessionDescriptorSupplierMixinTest {
    @Test
    void transientSessionDescriptorIsPreferredOverPersistedLookup() {
        var supplier = new DummySessionDescriptorSupplier();
        var transientDescriptor = new DummySessionDescriptor("transient");
        FcliExecutionContextHolder.pushNew();
        try {
            FcliExecutionContextHolder.current().getIsolationScope().setTransientSessionDescriptor(transientDescriptor);

            var result = supplier.getSessionDescriptor();

            assertSame(transientDescriptor, result);
            assertEquals(0, supplier.lookupCount);
        } finally {
            FcliExecutionContextHolder.pop();
        }
    }

    @Test
    void persistedLookupIsUsedIfNoTransientDescriptorExistsForType() {
        var supplier = new DummySessionDescriptorSupplier();
        FcliExecutionContextHolder.pushNew();
        try {
            FcliExecutionContextHolder.current().getIsolationScope().setTransientSessionDescriptor(new OtherSessionDescriptor());

            var result = supplier.getSessionDescriptor();

            assertEquals("persisted", result.value);
            assertEquals(1, supplier.lookupCount);
        } finally {
            FcliExecutionContextHolder.pop();
        }
    }

    private static final class DummySessionDescriptorSupplier extends AbstractSessionDescriptorSupplierMixin<DummySessionDescriptor> {
        private int lookupCount;

        @Override
        public ISessionNameSupplier getSessionNameSupplier() {
            return () -> "default";
        }

        @Override
        protected String getSessionDescriptorType() {
            return "dummy";
        }

        @Override
        protected DummySessionDescriptor getSessionDescriptor(String sessionName) {
            lookupCount++;
            return new DummySessionDescriptor("persisted");
        }
    }

    private static final class DummySessionDescriptor implements ISessionDescriptor {
        private final String value;

        private DummySessionDescriptor(String value) {
            this.value = value;
        }

        @Override
        public String getUrlDescriptor() {
            return value;
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
            return "dummy";
        }
    }

    private static final class OtherSessionDescriptor implements ISessionDescriptor {
        @Override
        public String getUrlDescriptor() {
            return "other";
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
            return "other";
        }
    }
}