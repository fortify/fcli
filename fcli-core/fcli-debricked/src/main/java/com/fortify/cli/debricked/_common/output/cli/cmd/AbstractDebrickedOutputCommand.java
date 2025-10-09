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
package com.fortify.cli.debricked._common.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.debricked._common.session.cli.mixin.DebrickedUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractDebrickedOutputCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IUnirestInstanceSupplier {
    @Getter @Mixin private DebrickedUnirestInstanceSupplierMixin unirestInstanceSupplier;
    
    public final UnirestInstance getUnirestInstance() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
}