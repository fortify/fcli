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
package com.fortify.cli.config.publickey.cli.mixin;

import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeyDescriptor;

import lombok.Getter;
import picocli.CommandLine.Parameters;

public class PublicKeyResolverMixin {
    
    public static abstract class AbstractPublicKeyResolverMixin  {
        public abstract String getNameOrFingerprint();

        public PublicKeyDescriptor getPublicKeyDescriptor(){
            return SignatureHelper.publicKeyTrustStore().load(getNameOrFingerprint(), true);
        }
    }
    
    public static class PositionalParameter extends AbstractPublicKeyResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "fcli.config.public-key.nameOrFingerprint")
        @Getter private String nameOrFingerprint;
    }
}
