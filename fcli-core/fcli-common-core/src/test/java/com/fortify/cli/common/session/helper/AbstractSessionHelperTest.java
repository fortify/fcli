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
package com.fortify.cli.common.session.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.util.FcliDataHelper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;

class AbstractSessionHelperTest {
    private static final String DATA_DIR_PROPERTY = "fcli.env.FCLI_DATA_DIR";
    private static final String STATE_DIR_PROPERTY = "fcli.env.FCLI_STATE_DIR";
    private static final String SESSION_NAME = "corrupt-session";

    @TempDir Path tempDir;
    private Path stateDir;
    private String previousDataDir;
    private String previousStateDir;
    private Level previousLevel;
    private boolean previousAdditive;
    private final TestSessionHelper helper = new TestSessionHelper();
    private final Logger log = (Logger) LoggerFactory.getLogger(AbstractSessionHelper.class);
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();

    @BeforeEach
    void setUp() {
        previousDataDir = System.getProperty(DATA_DIR_PROPERTY);
        previousStateDir = System.getProperty(STATE_DIR_PROPERTY);
        stateDir = tempDir.resolve("state");
        System.setProperty(DATA_DIR_PROPERTY, tempDir.toString());
        System.setProperty(STATE_DIR_PROPERTY, stateDir.toString());
        FcliExecutionContextHolder.pushNew();
        previousLevel = log.getLevel();
        previousAdditive = log.isAdditive();
        log.setLevel(Level.DEBUG);
        log.setAdditive(false);
        events.start();
        log.addAppender(events);
        FcliDataHelper.saveSecuredFile(descriptorPath(), "not-json", true);
        assertTrue(helper.exists(SESSION_NAME));
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(events);
        events.stop();
        log.setLevel(previousLevel);
        log.setAdditive(previousAdditive);
        FcliExecutionContextHolder.pop();
        if (previousDataDir == null) {
            System.clearProperty(DATA_DIR_PROPERTY);
        } else {
            System.setProperty(DATA_DIR_PROPERTY, previousDataDir);
        }
        if (previousStateDir == null) {
            System.clearProperty(STATE_DIR_PROPERTY);
        } else {
            System.setProperty(STATE_DIR_PROPERTY, previousStateDir);
        }
    }

    @Test
    void optionalLookupDeletesCorruptDescriptorAndWarnsOnceWithOriginalException() {
        assertNull(helper.get(SESSION_NAME, false));

        assertFalse(helper.exists(SESSION_NAME));
        assertEquals(1, events.list.size());
        var warning = events.list.get(0);
        assertEquals(Level.WARN, warning.getLevel());
        assertEquals("Error reading session descriptor from " + descriptorPath()
            + "; session descriptor has been deleted", warning.getFormattedMessage());
        var cause = assertInstanceOf(ThrowableProxy.class, warning.getThrowableProxy()).getThrowable();
        assertInstanceOf(JsonProcessingException.class, cause);
    }

    @Test
    void requiredLookupDeletesCorruptDescriptorAndThrowsWithoutLoggingAgain() {
        var exception = assertThrows(FcliTechnicalException.class, () -> helper.get(SESSION_NAME, true));

        assertEquals("Error reading session descriptor, please try logging in again", exception.getMessage());
        assertInstanceOf(JsonProcessingException.class, exception.getCause());
        assertFalse(helper.exists(SESSION_NAME));
        assertTrue(events.list.isEmpty(), "The caller reports the thrown exception");
    }

    private Path descriptorPath() {
        return stateDir.resolve("sessions").resolve(helper.getType()).resolve(SESSION_NAME);
    }

    private static final class TestSessionHelper extends AbstractSessionHelper<ISessionDescriptor> {
        @Override public String getType() { return "test-session"; }
        @Override protected String getLoginCmd() { return "fcli test session login"; }
        @Override protected Class<ISessionDescriptor> getSessionDescriptorType() { return ISessionDescriptor.class; }
    }
}
