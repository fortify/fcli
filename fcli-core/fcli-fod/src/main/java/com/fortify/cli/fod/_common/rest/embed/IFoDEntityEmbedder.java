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
package com.fortify.cli.fod._common.rest.embed;

import com.fasterxml.jackson.databind.node.ObjectNode;

import kong.unirest.UnirestInstance;

/**
 * Interface for executing one or more requests and adding the response data
 * as embedded properties to a given record.
 * 
 * @author rsenden
 */
@FunctionalInterface
public interface IFoDEntityEmbedder {
    void embed(UnirestInstance unirest, ObjectNode record);
}