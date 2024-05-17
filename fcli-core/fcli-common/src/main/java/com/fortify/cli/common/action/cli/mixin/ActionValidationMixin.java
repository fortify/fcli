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
package com.fortify.cli.common.action.cli.mixin;

import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler.ActionInvalidSchemaVersionHandler;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler.ActionInvalidSignatureHandler;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;

import lombok.Getter;
import picocli.CommandLine.Option;

public class ActionValidationMixin {
    @Option(names={"--on-invalid-signature"}, defaultValue = "prompt", descriptionKey="fcli.action.on-invalid-signature")
    @Getter private ActionInvalidSignatureHandler onInvalidSignature;
    @Option(names={"--on-unsigned"}, defaultValue = "prompt", descriptionKey="fcli.action.on-unsigned")
    @Getter private ActionInvalidSignatureHandler onUnsigned;
    @Option(names={"--on-no-public-key"}, defaultValue = "prompt", descriptionKey="fcli.action.on-no-public-key")
    @Getter private ActionInvalidSignatureHandler onNoPublicKey;
    @Option(names={"--on-invalid-version"}, defaultValue = "prompt", descriptionKey="fcli.action.on-invalid-version")
    @Getter private ActionInvalidSchemaVersionHandler onUnsupportedVersion;
    
    public ActionValidationHandler getActionValidationHandler() {
        return ActionValidationHandler.builder()
                .onSignatureStatus(SignatureStatus.MISMATCH, onInvalidSignature)
                .onSignatureStatus(SignatureStatus.NO_PUBLIC_KEY, onNoPublicKey)
                .onSignatureStatus(SignatureStatus.UNSIGNED, onUnsigned)
                .onUnsupportedSchemaVersion(onUnsupportedVersion)
                .build();
    }
}

