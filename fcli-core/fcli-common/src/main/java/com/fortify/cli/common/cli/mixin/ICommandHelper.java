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
package com.fortify.cli.common.cli.mixin;

import java.util.Optional;

import picocli.CommandLine.Model.CommandSpec;

/**
 * Lightweight abstraction extracted from {@link CommandHelperMixin} exposing just the helper
 * methods required by producer builders to inspect command, spec, mixins, and cast the command.
 */
public interface ICommandHelper {
    CommandSpec getCommandSpec();
    Object getCommand();
    <T> Optional<T> getCommandAs(Class<T> asType);
}