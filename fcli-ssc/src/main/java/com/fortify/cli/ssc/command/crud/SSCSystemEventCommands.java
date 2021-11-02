/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.command.crud;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.IOutputOptionsWriterConfigSupplier;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.common.picocli.component.output.OutputOptionsWriterConfig;
import com.fortify.cli.ssc.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.command.crud.SSCCrudRootCommands.SSCGetCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

public class SSCSystemEventCommands {
    private static final String NAME = "system-events";
    private static final String DESC = "system events";
    
    private static final String _getDefaultOutputColumns() {
		return "eventDate#userName#eventType#projectVersionId#entityId";
	}
	
    @ReflectiveAccess
	@SubcommandOf(SSCGetCommand.class)
	@Command(name = NAME, description = "Get "+DESC+" data from SSC")
	@RequiresProduct(ProductOrGroup.SSC)
	public static final class Get extends AbstractSSCUnirestRunnerCommand implements IOutputOptionsWriterConfigSupplier {
		@CommandLine.Mixin private OutputOptionsHandler outputOptionsHandler;
		
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			outputOptionsHandler.write(
				unirest.get("/api/v1/events?limit=-1")
					.accept("application/json")
					.header("Content-Type", "application/json")
			);
			
			return null;
		}
		
		@Override
		public OutputOptionsWriterConfig getOutputOptionsWriterConfig() {
			return SSCGetCommand.defaultOutputConfig().defaultColumns(_getDefaultOutputColumns());
		}
	}
}