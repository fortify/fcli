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
package com.fortify.cli.aviator.app.cli.mixin;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator.app.helper.AviatorAppHelper;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Holder for the mixins that specify an Aviator application on the command line. Each use case has
 * its own inner class, as the two use cases intentionally interpret values differently:
 * <ul>
 *  <li>{@link NameOrIdParameter}: admin commands ({@code fcli aviator app ...}), where the application
 *      is a positional parameter that may be an application name or id.</li>
 *  <li>{@link NameOnlyOption}: user-facing commands (for example {@code fcli aviator ssc audit-sast}),
 *      where the {@code --app} option is always an application name.</li>
 * </ul>
 * <p><b>Do not</b> add a name-or-id mixin for the user-facing {@code --app} option. That option has always been
 * a name, so interpreting numeric values as an id would silently change behavior for existing users and scripts
 * that use numeric application names. For the same reason, {@link NameOrIdParameter} treats numeric values as
 * ids, as the admin commands only accepted ids before names were supported.
 */
public final class AviatorAppResolverMixin {
    private AviatorAppResolverMixin() {}

    /**
     * <b>Admin commands only.</b> Positional parameter accepting an application name or id. Numeric values are
     * always interpreted as an id (no fallback to name), all other values as a name.
     */
    public static class NameOrIdParameter {
        @Parameters(index = "0", arity = "1", paramLabel = "<app-name-or-id>", descriptionKey = "fcli.aviator.app.app-name-or-id")
        @Getter private String appNameOrId;

        public final String getApplicationId(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor) {
            return AviatorAppHelper.resolveApplicationId(client, configDescriptor, appNameOrId);
        }
    }

    /**
     * <b>User-facing commands only.</b> Optional {@code --app} option whose value is always an application
     * name, even if it is numeric; it is never resolved as an id, to remain backward compatible.
     */
    public static class NameOnlyOption {
        @Option(names = {"--app"})
        @Getter private String appName;
    }
}
