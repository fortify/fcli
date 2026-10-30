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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

/**
 * Issue-specific evidence produced by an {@link IFoDIssueEvidenceMatcher}; the attribution basis
 * is determined by the attribution engine based on the cause and the issue status transition.
 */
public record FoDIssueEvidence(FoDAttributionCause cause, String evidence) {}
