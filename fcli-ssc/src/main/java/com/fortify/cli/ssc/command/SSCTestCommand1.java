/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.command;

import com.fortify.cli.command.RootCommand;
import com.fortify.cli.command.util.SubcommandOf;
import com.fortify.cli.session.command.consumer.LoginSessionConsumerMixin;
import com.fortify.cli.ssc.rest.unirest.SSCUnirestRunner;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Singleton @ReflectiveAccess
@SubcommandOf(RootCommand.class)
@Command(name = "ssc-test", description = "SSC test 1")
public class SSCTestCommand1 implements Runnable {
	@Getter @Inject private SSCUnirestRunner unirestRunner;
	@Getter @Mixin private LoginSessionConsumerMixin loginSessionConsumerMixin;

	@Override @SneakyThrows
	public void run() {
		unirestRunner.runWithUnirest(loginSessionConsumerMixin.getLoginSessionName(), this::runWithUnirest);
	}
	
	private Void runWithUnirest(UnirestInstance unirest) {
		unirest.get("/api/v1/events?limit=10")
		.accept("application/json")
		.header("Content-Type", "application/json")
		.asPaged(
				r->r.asJson(),
				r->getNextPageLink(r))
		.stream().map(HttpResponse::getBody).forEach(System.out::println);
		return null;
	}
	
	private String getNextPageLink(HttpResponse<Object> r) {
		return (String) ((JsonNode)r.getBody()).getObject().optQuery("/links/next/href");
	}
}
