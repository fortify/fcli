/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.action.cli.mixin;

import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.crypto.SignatureHelper.InvalidSignatureHandler;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

public class ActionResolverMixin {
    public static abstract class AbstractActionResolverMixin {
        @Mixin private ActionSourceResolverMixin.OptionalOption actionSourceResolver;
        public abstract String getAction();
        
        public Action loadAction(String type, InvalidSignatureHandler invalidSignatureHandler) {
            return ActionLoaderHelper.loadAction(actionSourceResolver.getActionSources(type), getAction(), invalidSignatureHandler);
        }
        
        public String loadActionContents(String type, InvalidSignatureHandler invalidSignatureHandler) {
            return ActionLoaderHelper.loadActionContents(actionSourceResolver.getActionSources(type), getAction(), invalidSignatureHandler);
        }
    }
    
    public static class RequiredParameter extends AbstractActionResolverMixin {
        @Getter @Parameters(arity="1", descriptionKey="fcli.action.nameOrLocation") private String action;
    }
    
    public static class OptionalParameter extends AbstractActionResolverMixin {
        @Getter @Parameters(arity="0..1", descriptionKey="fcli.action.nameOrLocation") private String action;
    }
}
