/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
/**
 * This package provides generic functionality for outputting data to various output formats. At the moment, this 
 * output framework accepts {@link com.fasterxml.jackson.databind.JsonNode} instances as its input, which is 
 * appropriate for almost all commands working with REST API's. Potentially in the future we may need to add
 * support for other input formats, for example raw data produced by 3rd-party tools that are being run by fcli.
 * The actual output format implementations are defined in the appropriate sub-packages. 
 */
package com.fortify.cli.common.output.writer.record;

