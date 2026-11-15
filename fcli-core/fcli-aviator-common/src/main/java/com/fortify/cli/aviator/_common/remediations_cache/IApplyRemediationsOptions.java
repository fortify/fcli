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
package com.fortify.cli.aviator._common.remediations_cache;

import java.util.List;

import com.fortify.cli.aviator.fpr.remediation.ImplicitFixApprover;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;

/** Abstraction over the shared apply-remediations CLI options, allowing RemediationsApplyHelper
 *  to remain independent of concrete Picocli types. */
public interface IApplyRemediationsOptions {
    String getSourceCodeDirectory();
    List<String> getIssueIds();
    ISourceDecoder getSourceDecoder();
    boolean isPreviewMode();

    /** Decides whether fixes that were not requested but are needed to resolve requested issues may be applied. */
    default ImplicitFixApprover getImplicitFixApprover() {
        return ImplicitFixApprover.approveAll();
    }
    void validate();
}
