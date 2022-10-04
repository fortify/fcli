package com.fortify.cli.sc_dast.rest.cli.cmd;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.cli.cmd.AbstractJsonGetCommand;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.sc_dast.rest.cli.mixin.SCDastUnirestRunnerMixin;
import com.fortify.cli.sc_dast.util.SCDastInputTransformer;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * This base class for 'get' {@link Command} implementations provides capabilities for running 
 * SC-DAST requests and outputting detailed results.
 *   
 * @author rsenden
 */
@ReflectiveAccess @FixInjection
public abstract class AbstractSCDastJsonGetCommand extends AbstractJsonGetCommand {
    @Getter @Mixin private SCDastUnirestRunnerMixin unirestRunner;
    @Getter private UnaryOperator<JsonNode> inputTransformer = SCDastInputTransformer::getItems;
}