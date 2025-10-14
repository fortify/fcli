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
package com.fortify.cli.common.action.model;

/**
 * Interface that can be implemented by action elements contained in Map's,
 * allowing them to be informed about their map key.
 */
public interface IMapKeyAware<T> {
    void setKey(T key);
    T getKey();
}
