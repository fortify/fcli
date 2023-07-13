/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.common.output.writer.output.standard;

import picocli.CommandLine.ITypeConverter;

public final class VariableStoreConfigConverter implements ITypeConverter<VariableStoreConfig> {
    @Override
    public VariableStoreConfig convert(String value) throws Exception {
        int pos = value.indexOf(':');
        String variableName = pos==-1 ? value : value.substring(0, pos);
        String options = pos==-1 ? null : value.substring(pos+1);
        return new VariableStoreConfig(variableName, options);
    }
}