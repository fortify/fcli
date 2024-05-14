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
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionLoadResult;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins.AbstractTextResolverMixin;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class ActionResolverMixin {
    public static abstract class AbstractActionResolverMixin {
        @Getter @Mixin private ActionSourceResolverMixin.OptionalOption actionSourceResolver;
        @Mixin private PublicKeyResolverMixin publicKeyResolver;
        public abstract String getAction();
        
        public ActionLoadResult load(String type, ActionValidationHandler actionValidationHandler) {
            var action = getAction();
            return action==null 
                ? null 
                : ActionLoaderHelper.load(actionSourceResolver.getActionSources(type), 
                        action, actionValidationHandler.toBuilder().extraPublicKey(publicKeyResolver.getText()).build());
        }
        
        public Action loadAction(String type, ActionValidationHandler actionValidationHandler) {
            return load(type, actionValidationHandler).asAction();
        }
        
        public String loadActionContents(String type, ActionValidationHandler actionValidationHandler) {
            return load(type, actionValidationHandler).asText();
        }
    }
    
    public static class RequiredParameter extends AbstractActionResolverMixin {
        @Getter @Parameters(arity="1", descriptionKey="fcli.action.nameOrLocation") private String action;
    }
    
    public static class OptionalParameter extends AbstractActionResolverMixin {
        @Getter @Parameters(arity="0..1", descriptionKey="fcli.action.nameOrLocation") private String action;
    }
    
    private static class PublicKeyResolverMixin extends AbstractTextResolverMixin {
        @Getter @Option(names={"--pubkey"}, required = false, descriptionKey = "fcli.action.resolver.pubkey", paramLabel = "source") private String textSource;
    }
}
