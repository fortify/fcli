/**
 * <p>This package contains many base classes for command implementations; this includes both
 * generic classes and based classes for common entity command implementations like 'list', 
 * 'get', 'delete', 'enable', ...</p>
 * 
 * <p>For most abstract commands, there are two variants; one for commands that generate
 * {@link kong.unirest.HttpRequest} instances to be executed, outputting the response to
 * an {@link com.fortify.cli.common.output.cli.mixin.OutputMixin}, identified by 
 * AbstractHttp*Command, and one for commands that generate actual data as 
 * {@link com.fasterxml.jackson.databind.JsonNode} instances, identified by AbstractJson*Command.</p> 
 * 
 * <p>Usually, each product module has it's own generic abstract classes that extend from the
 * classes in this package, adding generic product-specific behavior. Actual command
 * implementations would then extend from these product-specific abstract classes.</p>
 * 
 * <p>In general, common entity commands should extend from the appropriate abstract command.
 * For example, most or all 'list' commands should extend Abstract[product][type]ListCommand.
 * If there is no abstract base class for a particular entity verb or action, commands
 * should extend from Abstract[product]HttpOutputCommand or Abstract[product]JsonOutputCommand.</p>
 * 
 * TODO These classes may require some refactoring for SC SAST; currently these classes are based
 *      on a single {@link com.fortify.cli.common.rest.runner.IUnirestRunner}, however SC SAST
 *      will need to cover both SSC and SC SAST API endpoints, requiring two separate 
 *      {@link com.fortify.cli.common.rest.runner.IUnirestRunner} instances.
 */
package com.fortify.cli.common.rest.cli.cmd;

