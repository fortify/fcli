/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.action.runner.n;

import com.fortify.cli.common.action.cli.mixin.ActionResolverMixin.AbstractActionResolverMixin;
import com.fortify.cli.common.action.cli.mixin.ActionSourceResolverMixin.AbstractActionSourceResolverMixin;
import com.fortify.cli.common.action.model.Action;

import lombok.Getter;

public class ActionSourceConfig {
    private final AbstractActionResolverMixin actionResolverMixin;
    private final AbstractActionSourceResolverMixin actionSourceMixin;
    @Getter private final Action action;
    
    public ActionSourceConfig(AbstractActionSourceResolverMixin actionSourceMixin, Action action) {
        this.actionSourceMixin = actionSourceMixin;
        this.action = action;
        this.actionResolverMixin = null;
    }

    public ActionSourceConfig(AbstractActionResolverMixin actionResolverMixin, Action action) {
        this.actionResolverMixin = actionResolverMixin;
        this.action = action;
        this.actionSourceMixin = null;
    }
    
    public final String asArgsString() {
        return actionResolverMixin!=null 
                ? actionResolverMixin.asArgsString()
                : actionSourceMixin.asArgsString();
    }
}
