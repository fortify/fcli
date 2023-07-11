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
package com.fortify.cli.util.entity.crypto.cli.cmd;

import java.util.Scanner;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;

import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;

public abstract class AbstractCryptoCommand extends AbstractFortifyCLICommand implements Runnable {
    @Mixin private CommandHelperMixin commandHelper;
    
    @Override @SneakyThrows
    public final void run() {
        initMixins();
        String prompt = commandHelper.getMessageResolver().getMessageString("prompt")+" ";
        String value;
        if ( System.console()!=null ) {
            value = new String(System.console().readPassword(prompt));
        } else {
            try ( var scanner = new Scanner(System.in) ) {
                System.out.print(prompt);
                value = scanner.nextLine();
            }
        }
        System.out.println(process(value));
    }

    protected abstract String process(String value);
}
