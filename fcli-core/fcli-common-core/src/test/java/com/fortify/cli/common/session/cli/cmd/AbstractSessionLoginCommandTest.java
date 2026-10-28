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
package com.fortify.cli.common.session.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.session.cli.mixin.ISessionNameSupplier;
import com.fortify.cli.common.session.helper.AbstractSessionHelper;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

class AbstractSessionLoginCommandTest {
    private static final String FCLI_DATA_DIR_PROPERTY = "fcli.env.FCLI_DATA_DIR";
    private static final String SESSION_NAME = "test-session";

    @TempDir Path fcliDataDir;
    private String previousFcliDataDir;
    private final TestSessionHelper sessionHelper = new TestSessionHelper();

    @BeforeEach
    void setUp() {
        previousFcliDataDir = System.getProperty(FCLI_DATA_DIR_PROPERTY);
        System.setProperty(FCLI_DATA_DIR_PROPERTY, fcliDataDir.toString());
        FcliExecutionContextHolder.pushNew();
    }

    @AfterEach
    void tearDown() {
        FcliExecutionContextHolder.pop();
        if ( previousFcliDataDir==null ) {
            System.clearProperty(FCLI_DATA_DIR_PROPERTY);
        } else {
            System.setProperty(FCLI_DATA_DIR_PROPERTY, previousFcliDataDir);
        }
    }

    @Test
    void defaultLifecycleDestroysExistingSessionBeforeLogin() {
        sessionHelper.save(SESSION_NAME, new TestSessionDescriptor("old"));
        assertTrue(sessionHelper.exists(SESSION_NAME));
        var command = new TestLoginCommand(sessionHelper, null);

        assertThrows(FcliSimpleException.class, command::getJsonNode);

        assertFalse(sessionHelper.exists(SESSION_NAME));
        assertEquals(1, command.logoutCount);
    }

    @Test
    void optOutKeepsExistingSessionWhenLoginFails() {
        sessionHelper.save(SESSION_NAME, new TestSessionDescriptor("old"));
        TestLoginCommand command = new OptOutTestLoginCommand(sessionHelper, null);

        assertThrows(FcliSimpleException.class, command::getJsonNode);

        assertTrue(sessionHelper.exists(SESSION_NAME));
        assertEquals("old", sessionHelper.get(SESSION_NAME, true).getValue());
        assertEquals(0, command.logoutCount);
    }

    @Test
    void optOutCreatesNoSessionWhenLoginFailsForNewName() {
        TestLoginCommand command = new OptOutTestLoginCommand(sessionHelper, null);

        assertThrows(FcliSimpleException.class, command::getJsonNode);

        assertFalse(sessionHelper.exists(SESSION_NAME));
    }

    @Test
    void optOutReplacesExistingSessionWhenLoginSucceeds() {
        sessionHelper.save(SESSION_NAME, new TestSessionDescriptor("old"));
        TestLoginCommand command = new OptOutTestLoginCommand(sessionHelper, new TestSessionDescriptor("new"));

        command.getJsonNode();

        assertEquals("new", sessionHelper.get(SESSION_NAME, true).getValue());
        assertEquals(0, command.logoutCount);
    }

    private static class TestLoginCommand extends AbstractSessionLoginCommand<TestSessionDescriptor> {
        private final TestSessionHelper sessionHelper;
        private final TestSessionDescriptor loginResult;
        private int logoutCount;

        private TestLoginCommand(TestSessionHelper sessionHelper, TestSessionDescriptor loginResult) {
            this.sessionHelper = sessionHelper;
            this.loginResult = loginResult;
        }

        @Override
        public ISessionNameSupplier getSessionNameSupplier() {
            return () -> SESSION_NAME;
        }

        @Override
        protected void logoutBeforeNewLogin(String sessionName, TestSessionDescriptor sessionDescriptor) {
            logoutCount++;
        }

        @Override
        protected TestSessionDescriptor login(String sessionName) {
            if ( loginResult==null ) {
                throw new FcliSimpleException("Validation failed");
            }
            return loginResult;
        }

        @Override
        protected AbstractSessionHelper<TestSessionDescriptor> getSessionHelper() {
            return sessionHelper;
        }

        @Override
        public IOutputHelper getOutputHelper() {
            return null;
        }
    }

    private static final class OptOutTestLoginCommand extends TestLoginCommand {
        private OptOutTestLoginCommand(TestSessionHelper sessionHelper, TestSessionDescriptor loginResult) {
            super(sessionHelper, loginResult);
        }

        @Override
        protected boolean shouldReplaceExistingSessionBeforeLogin() {
            return false;
        }
    }

    private static final class TestSessionHelper extends AbstractSessionHelper<TestSessionDescriptor> {
        @Override
        public String getType() {
            return "test-login";
        }

        @Override
        protected String getLoginCmd() {
            return "fcli test session login";
        }

        @Override
        protected Class<TestSessionDescriptor> getSessionDescriptorType() {
            return TestSessionDescriptor.class;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TestSessionDescriptor implements ISessionDescriptor {
        private String value;

        public TestSessionDescriptor() {}

        TestSessionDescriptor(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String getUrlDescriptor() {
            return "test";
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
            return "test-login";
        }
    }
}
