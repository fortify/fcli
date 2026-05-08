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
package com.fortify.cli.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.json.JsonHelper;

public class FcliConcurrencyTest {

    @Test
    public void actionVarsAreIsolatedPerInvocation() throws InterruptedException, ExecutionException {
        ExecutorService ex = Executors.newFixedThreadPool(2);
        Callable<String> task1 = () -> {
            FcliExecutionContextHolder.pushNew();
            try {
                var cli = JsonHelper.getObjectMapper().createObjectNode();
                var vars = new ActionRunnerVars(null, cli);
                vars.set("global.foo", new TextNode("v1"));
                return FcliExecutionContextHolder.current().getActionState().getGlobalActionValues().get("foo").asText();
            } finally {
                FcliExecutionContextHolder.pop();
            }
        };
        Callable<String> task2 = () -> {
            FcliExecutionContextHolder.pushNew();
            try {
                var cli = JsonHelper.getObjectMapper().createObjectNode();
                var vars = new ActionRunnerVars(null, cli);
                vars.set("global.foo", new TextNode("v2"));
                return FcliExecutionContextHolder.current().getActionState().getGlobalActionValues().get("foo").asText();
            } finally {
                FcliExecutionContextHolder.pop();
            }
        };
        Future<String> f1 = ex.submit(task1);
        Future<String> f2 = ex.submit(task2);
        String r1 = f1.get();
        String r2 = f2.get();
        assertEquals("v1", r1);
        assertEquals("v2", r2);
        assertNotEquals(r1, r2);
        ex.shutdownNow();
    }

    @Test
    public void outputDelegationIsThreadLocal() throws Exception {
        StdioHelper.install();
        ExecutorService ex = Executors.newFixedThreadPool(2);
        Callable<String> t1 = () -> {
            var baos = new ByteArrayOutputStream();
            var ps = new PrintStream(baos, true);
            StdioHelper.pushOut(ps);
            System.out.println("hello-1");
            ps.flush();
            StdioHelper.popOut();
            return baos.toString();
        };
        Callable<String> t2 = () -> {
            var baos = new ByteArrayOutputStream();
            var ps = new PrintStream(baos, true);
            StdioHelper.pushOut(ps);
            System.out.println("hello-2");
            ps.flush();
            StdioHelper.popOut();
            return baos.toString();
        };
        var f1 = ex.submit(t1);
        var f2 = ex.submit(t2);
        String a1 = f1.get();
        String a2 = f2.get();
        ex.shutdownNow();
        // Expect each thread's output to contain its own message only
        assertEquals(true, a1.contains("hello-1"));
        assertEquals(true, a2.contains("hello-2"));
    }
}
