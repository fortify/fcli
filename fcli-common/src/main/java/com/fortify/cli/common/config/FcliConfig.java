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
package com.fortify.cli.common.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.util.FcliHomeHelper;

import io.micronaut.core.util.StringUtils;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

// TODO Use SneakThrows or use proper exception handling?
@Singleton
public class FcliConfig {
	private static final Path CONFIG_PATH = Paths.get("config.json");
	private final ObjectMapper objectMapper; 
	private final Map<String,String> config = new HashMap<>();
	private boolean dirty = false;
	
	@Inject
	public FcliConfig(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		load();
	}
	
	public void set(String name, String value) {
		config.put(name, value);
		dirty = true;
	}
	
	public String get(String name) {
		return config.get(name);
	}
	
	public boolean contains(String name) {
		return config.containsKey(name);
	}
	
	public Map<String,String> all() {
		return Collections.unmodifiableMap(config);
	}
	
	@SneakyThrows
	private void load() {
		String configString = FcliHomeHelper.readFile(CONFIG_PATH, false);
		if ( StringUtils.isNotEmpty(configString) ) {
			loadFromJson(configString);
		}
	}

	@PreDestroy @SneakyThrows
	public void save() {
		if ( dirty ) {
			FcliHomeHelper.saveFile(CONFIG_PATH, getAsJson());
		}
		dirty = false;
	}
	
	@SneakyThrows
	private final String getAsJson() {
		List<ConfigProperty> configPropertyList = config.entrySet().stream().map(this::mapEntryToConfigProperty).collect(Collectors.toList());
		return objectMapper.writeValueAsString(configPropertyList);
	}
	
	@SneakyThrows
	private void loadFromJson(String configString) {
		config.clear();
		ConfigProperty[] configPropertyArray = objectMapper.readValue(configString, ConfigProperty[].class);
		Stream.of(configPropertyArray).forEach(this::addMapEntry);
	}
	
	private final ConfigProperty mapEntryToConfigProperty(Map.Entry<String, String> entry) {
		return new ConfigProperty(entry.getKey(), entry.getValue());
	}
	
	private final void addMapEntry(ConfigProperty configProperty) {
		config.put(configProperty.getKey(), configProperty.getValue()); 
	}
	
	@AllArgsConstructor @NoArgsConstructor @Data
	private static final class ConfigProperty {
		private String key, value;
	}
}
