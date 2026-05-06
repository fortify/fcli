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
package com.fortify.cli.fod._common.cli.mixin;

import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * Argument group for commands that require either an application or release as
 * target. This is a common pattern in FoD commands, where some commands can
 * target either an application or a release, and the user can specify either
 * one of them. This argument group allows for defining both options in a
 * mutually exclusive way, and provides getters for both the app and release
 * targets. The app target is resolved using the FoDAppResolverMixin, while the
 * release target is resolved using the
 * FoDReleaseByQualifiedNameOrIdResolverMixin. This argument group can be used
 * in any command that needs to support both app and release targeting, without
 * having to duplicate the logic for resolving the targets.
 * 
 * @author Sangamesh Vijaykumar
 */
public final class FoDAppOrReleaseArgGroup {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private FoDAppArgGroup app = new FoDAppArgGroup();

    @ArgGroup(exclusive = false, multiplicity = "1", order = 2)
    @Getter private FoDReleaseArgGroup release = new FoDReleaseArgGroup();

    public static class FoDAppArgGroup extends FoDAppResolverMixin.AbstractFoDAppResolverMixin {
        @Option(
            names = { "--app" },
            required = true,
            descriptionKey = "fcli.fod.app.app-name-or-id"
        )
        @Getter private String appNameOrId;
    }

    public static class FoDReleaseArgGroup
            extends FoDReleaseByQualifiedNameOrIdResolverMixin.AbstractFoDQualifiedReleaseNameOrIdResolverMixin {
        @Option(
            names = { "--release", "--rel" },
            required = true,
            paramLabel = "id|app[:ms]:rel",
            descriptionKey = "fcli.fod.release.resolver.name-or-id"
        )
        @Getter private String qualifiedReleaseNameOrId;
    }
}