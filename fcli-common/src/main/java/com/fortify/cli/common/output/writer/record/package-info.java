/**
 * This package provides generic functionality for outputting data to various output formats. At the moment, this 
 * output framework accepts {@link com.fasterxml.jackson.databind.JsonNode} instances as its input, which is 
 * appropriate for almost all commands working with REST API's. Potentially in the future we may need to add
 * support for other input formats, for example raw data produced by 3rd-party tools that are being run by fcli.
 * The actual output format implementations are defined in the appropriate sub-packages. 
 */
package com.fortify.cli.common.output.writer.record;

