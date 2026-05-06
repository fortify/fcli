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

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;

/**
 * Mixin that allows specifying either an application or a release as target for
 * a command. The application and release are specified using the same syntax as
 * the corresponding resolvers (FoDAppResolverMixin and
 * FoDReleaseByQualifiedNameOrIdResolverMixin, respectively), and the mixin
 * ensures that exactly one of them is specified. The mixin also propagates the
 * delimiter mixin to the release resolver, so that the same delimiter can be
 * used for both application and release specification. This mixin is intended
 * to be used in commands that can target either an application or a release,
 * and that need to resolve the corresponding IDs based on the specified names
 * or IDs.
 * 
 * @author Sangamesh Vijaykumar
 */
public final class FoDAppOrReleaseMixin implements IFoDDelimiterMixinAware {
    @ArgGroup(exclusive = true, multiplicity = "1", order = 1)
    @Getter private FoDAppOrReleaseArgGroup fodAppOrReleaseArgGroup = new FoDAppOrReleaseArgGroup();

    @Override
    public void setDelimiterMixin(FoDDelimiterMixin delimiterMixin) {
        FoDAppOrReleaseArgGroup.FoDReleaseArgGroup release = fodAppOrReleaseArgGroup.getRelease();
        if (release != null) {
            release.setDelimiterMixin(delimiterMixin);
        }
    }

    public boolean isAppSpecified() {
        FoDAppOrReleaseArgGroup.FoDAppArgGroup app = fodAppOrReleaseArgGroup.getApp();
        return app != null && app.getAppNameOrId() != null;
    }

    public boolean isReleaseSpecified() {
        FoDAppOrReleaseArgGroup.FoDReleaseArgGroup release = fodAppOrReleaseArgGroup.getRelease();
        return release != null && release.getQualifiedReleaseNameOrId() != null;
    }

    public String getAppId(UnirestInstance unirest) {
        FoDAppOrReleaseArgGroup.FoDAppArgGroup app = fodAppOrReleaseArgGroup.getApp();
        return app == null ? null : app.getAppId(unirest);
    }

    public String getReleaseId(UnirestInstance unirest) {
        FoDAppOrReleaseArgGroup.FoDReleaseArgGroup release = fodAppOrReleaseArgGroup.getRelease();
        return release == null ? null : release.getReleaseId(unirest);
    }
}