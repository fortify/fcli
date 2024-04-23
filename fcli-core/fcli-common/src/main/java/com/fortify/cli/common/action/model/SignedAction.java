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
package com.fortify.cli.common.action.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data
public class SignedAction implements IActionElement {
    /** Actual signature */
    private String signature;
    /** Public key fingerprint */
    private String publicKeyFingerprint;
    /** Additional information about action or signature */
    private ObjectNode info;
    /** Action contents as string, to avoid canonicalization issues */
    private String actionBase64;
    
    /**
     * {@link IActionElement#postLoad(Action)} implementation
     * for this signed action element, checking required elements
     * are present.
     */
    @Override
    public final void postLoad(Action action) {
        Action.checkNotBlank("signature", signature, this);
        Action.checkNotBlank("public key fingerprint", publicKeyFingerprint, this);
        Action.checkNotBlank("signed action", actionBase64, this);
    }
}
