/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_sast.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public abstract class AbstractSCSastControllerBaseRequestOutputCommand extends AbstractSCSastControllerOutputCommand  implements IBaseRequestSupplier {
    @Override
    public final HttpRequest<?> getBaseRequest() {
        return getBaseRequest(getUnirestInstance());
    }

    protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest);
}
