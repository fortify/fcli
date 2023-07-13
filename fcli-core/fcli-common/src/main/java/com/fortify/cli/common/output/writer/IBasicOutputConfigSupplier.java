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
package com.fortify.cli.common.output.writer;

import com.fortify.cli.common.output.cli.mixin.AbstractOutputHelperMixin;
import com.fortify.cli.common.output.writer.output.standard.IOutputConfigSupplier;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

/**
 * Interface for supplying a basic output configuration which may not have
 * been fully configured yet. For example, {@link AbstractOutputHelperMixin} will 
 * update the basic output configuration, like adding transformers from 
 * various sources.
 * 
 * Note that this interface is very similar to {@link IOutputConfigSupplier}, 
 * but has a different purpose.
 * 
 * @author rsenden
 *
 */
public interface IBasicOutputConfigSupplier {
    StandardOutputConfig getBasicOutputConfig();
}
