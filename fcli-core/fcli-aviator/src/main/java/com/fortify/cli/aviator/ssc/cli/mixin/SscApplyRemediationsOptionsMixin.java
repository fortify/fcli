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
package com.fortify.cli.aviator.ssc.cli.mixin;

import com.fortify.cli.aviator._common.cli.mixin.AbstractApplyRemediationsOptionsMixin;

import lombok.Getter;
import picocli.CommandLine.Mixin;

/**
 * SSC-specific apply-remediations options mixin. Composes SSC source selection with shared options
 * and provides SSC-specific validation logic.
 */
@Getter
public final class SscApplyRemediationsOptionsMixin extends AbstractApplyRemediationsOptionsMixin {
    @Mixin
    private AviatorSSCApplyRemediationsSourceMixin sourceSelector;

    @Override
    protected void validateSourceSelection() {
        sourceSelector.validate();
    }

    @Override
    protected boolean isCacheMode() {
        return sourceSelector.isFromCacheSelected();
    }
}
