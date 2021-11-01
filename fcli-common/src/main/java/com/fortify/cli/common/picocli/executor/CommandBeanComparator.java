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

import java.util.Comparator;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.inject.BeanDefinition;
import picocli.CommandLine.Command;

/**
 * Order bean definitions based on annotation metadata, using the following rules:
 * <ol>
 *  <li>If both beans have the {@link Order} annotation, order them based on {@link Order#value()}</li>
 *  <li>If only one of the beans has the {@link Order}, it will come before any bean that doesn't have the {@link Order} annotation</li>
 *  <li>If neither bean has the {@link Order}, but both have the {@link Command} annotation, they will be ordered by command name</li>
 *  <li>In any other situation, beans will be considered equal</li>
 * </ul>
 */
public class CommandBeanComparator implements Comparator<BeanDefinition<?>> {
	public static final CommandBeanComparator INSTANCE = new CommandBeanComparator();
	@Override
	public int compare(BeanDefinition<?> bd1, BeanDefinition<?> bd2) {
		AnnotationMetadata bd1am = bd1.getAnnotationMetadata();
		AnnotationMetadata bd2am = bd2.getAnnotationMetadata();
		if ( bd1am.hasAnnotation(Order.class) && bd2am.hasAnnotation(Order.class) ) {
			return Integer.compare(OrderUtil.getOrder(bd1am), OrderUtil.getOrder(bd2am));
		} else if ( bd1am.hasAnnotation(Order.class) ) {
			return -1;
		} else if ( bd2am.hasAnnotation(Order.class) ) {
			return 1;
		} else if (bd1am.hasAnnotation(Command.class) && bd2am.hasAnnotation(Command.class) ){
			String bd1name = bd1am.stringValue(Command.class, "name").orElseThrow();
			String bd2name = bd2am.stringValue(Command.class, "name").orElseThrow();
			return bd1name.compareTo(bd2name);
		} else {
			return 0;
		}
	}

}
