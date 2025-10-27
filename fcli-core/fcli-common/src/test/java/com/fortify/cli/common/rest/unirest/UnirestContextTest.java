/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.rest.unirest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class UnirestContextTest {

    @Test
    void lifecycleCreatesAndClosesInstances() {
        UnirestContext context = new UnirestContext();
        var i1 = context.getUnirestInstance("a", u->{});
        var i2 = context.getUnirestInstance("a", u->{});
        var i3 = context.getUnirestInstance("b", u->{});
        assertSame(i1, i2);
        assertNotSame(i1, i3);
        assertEquals(2, context.getCachedInstanceCount());
        context.close();
        // After close, attempting to create/get a new instance should fail
        assertThrows(IllegalStateException.class, () -> context.getUnirestInstance("c", u->{}));
    }

    // With explicit injection design, context isn't implicitly propagated. Verify independent contexts.
    @Test
    void separateContextsAreIndependent() {
        try (UnirestContext c1 = new UnirestContext(); UnirestContext c2 = new UnirestContext()) {
            var i1 = c1.getUnirestInstance("x", u->{});
            var i2 = c2.getUnirestInstance("x", u->{});
            assertNotSame(i1, i2);
            assertEquals(1, c1.getCachedInstanceCount());
            assertEquals(1, c2.getCachedInstanceCount());
        }
    }

    @Test
    void manualSharingAcrossThreadsRequiresExplicitPassing() throws Exception {
        try (UnirestContext context = new UnirestContext()) {
            AtomicBoolean ok = new AtomicBoolean(false);
            Thread t = new Thread(() -> {
                var inst = context.getUnirestInstance("thread", u->{});
                ok.set(inst != null);
            });
            t.start();
            t.join(5000);
            assertTrue(ok.get());
            assertEquals(1, context.getCachedInstanceCount());
        }
    }
}
