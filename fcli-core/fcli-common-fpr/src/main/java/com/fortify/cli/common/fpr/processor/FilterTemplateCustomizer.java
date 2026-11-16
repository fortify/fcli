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
package com.fortify.cli.common.fpr.processor;

import org.w3c.dom.Document;

import com.fortify.cli.common.fpr.filter.FilterTemplate;

/**
 * Hook invoked by {@link FilterTemplateParser} after {@code filtertemplate.xml} has been parsed,
 * allowing callers to adjust the parsed model and the underlying DOM, for example to add
 * product-specific tag definitions.
 */
@FunctionalInterface
public interface FilterTemplateCustomizer {
    FilterTemplateCustomizer NONE = (filterTemplate, document) -> {};

    void customize(FilterTemplate filterTemplate, Document document);
}
