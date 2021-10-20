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
package com.fortify.cli.rest.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.Getter;

@Singleton
public class UnirestInstanceFactory {
	@Getter private final ObjectMapper objectMapper;
	private final Map<String, UnirestInstance> instances = new HashMap<>();
	
	@Inject
	public UnirestInstanceFactory(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	public final UnirestInstance getUnirestInstance(String name) {
		return instances.computeIfAbsent(name, this::createUnirestInstance);
	}
	
	public final void closeUnirestInstance(String name) {
		UnirestInstance instance = instances.remove(name);
		instance.close();
	}
	
	private final UnirestInstance createUnirestInstance(String name) {
		UnirestInstance instance = Unirest.spawnInstance();
		instance.config().setObjectMapper(new JacksonObjectMapper(objectMapper));
		return instance;
	}
	
	@PreDestroy
	public void close() {
		instances.keySet().parallelStream().collect(Collectors.toSet()).forEach(this::closeUnirestInstance);
	}
}
