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
package com.fortify.cli.util.crypto.cli.cmd;

import com.fortify.cli.common.crypto.EncryptionHelper;

import picocli.CommandLine.Command;

@Command(name = "encrypt")
public final class CryptoEncryptCommand extends AbstractCryptoCommand {
    @Override
    protected String process(String value) {
        return EncryptionHelper.encrypt(value);
    }
}