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
package com.fortify.cli.common.picocli.executor;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;

import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Inject;

public class SubcommandsMap {
	private final LinkedHashMap<Class<?>, List<Object>> parentToSubcommandsMap = new LinkedHashMap<>();
	private final ApplicationContext applicationContext;
	private final EnabledCommandBeansHelper enabledCommandBeansHelper;
	
	@Inject
	public SubcommandsMap(ApplicationContext applicationContext,
			EnabledCommandBeansHelper enabledCommandBeansHelper) {
		this.applicationContext = applicationContext;
		this.enabledCommandBeansHelper = enabledCommandBeansHelper;
		build();
	}
	
	private final void build() {
		final var beanDefinitions = applicationContext.getBeanDefinitions(Qualifiers.byStereotype(SubcommandOf.class));
		beanDefinitions.stream()
		// TODO Instead of filtering disabled commands, it would be better to replace these with a generic, hidden command
		//      that tells the user that the command has been disabled (and for what reason). Requires some more research 
		//      though on how to properly do this.
			.filter(enabledCommandBeansHelper::isEnabled) 
			.sorted(CommandBeanComparator.INSTANCE)
			.forEach(bd ->
				addMultiValueEntry(
						parentToSubcommandsMap, 
						getParentCommandClazz(bd),
						applicationContext.getBean(bd)));
	}
	
	private static final Class<?> getParentCommandClazz(BeanDefinition<?> bd) {
		Optional<Class<?>> optClazz = bd.getAnnotation(SubcommandOf.class).classValue();
		if ( !optClazz.isPresent() ) {
			throw new IllegalStateException("No parent command found for class "+bd.getBeanType().getName());
		}
		return optClazz.get();
	}

	private static final <K, V> void addMultiValueEntry(LinkedHashMap<K, List<V>> map, K key, V value) {
		map.computeIfAbsent(key, k->new LinkedList<V>()).add(value);
	}

	public List<Object> get(Class<?> parentCommandClazz) {
		return parentToSubcommandsMap.get(parentCommandClazz);
	}
}
