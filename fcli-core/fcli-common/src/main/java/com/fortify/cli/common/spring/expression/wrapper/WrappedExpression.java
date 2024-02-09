/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates, a Micro Focus company
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
package com.fortify.cli.common.spring.expression.wrapper;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;

/**
 * <p>This is a simple wrapper class for a Spring {@link Expression}
 * instance. This class is used as a based class for both 
 * {@link SimpleExpression} and {@link TemplateExpression}.</p>
 */
public class WrappedExpression implements Expression {
	private final Expression target;
	
	/**
	 * Constructor for configuring the expression to be wrapped
	 * @param target {@link Expression} to be wrapped
	 */
	public WrappedExpression(Expression target) {
		this.target = target;
	}
	
	/**
	 * @see org.springframework.expression.Expression#getValue()
	 * @return The evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public Object getValue() throws EvaluationException {
		return target.getValue();
	}

	/**
	 * @see org.springframework.expression.Expression#getValue(Object)
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public Object getValue(Object rootObject) throws EvaluationException {
		return target.getValue(rootObject);
	}

	/**
	 * @see org.springframework.expression.Expression#getValue(java.lang.Class)
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public <T> T getValue(Class<T> desiredResultType) throws EvaluationException {
		return target.getValue(desiredResultType);
	}

	/**
	 * @see org.springframework.expression.Expression#getValue(Object, java.lang.Class)
	 * @param rootObject the root object against which to evaluate the expression
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public <T> T getValue(Object rootObject,Class<T> desiredResultType) throws EvaluationException {
		return target.getValue(rootObject, desiredResultType);
	}

	/**
	 * @see org.springframework.expression.Expression#getValue(EvaluationContext)
	 * @param context the context in which to evaluate the expression
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 * 
	 */
	public Object getValue(EvaluationContext context) throws EvaluationException {
		return target.getValue(context);
	}

	/**
	 * @see org.springframework.expression.Expression#getValue(EvaluationContext, Object)
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 * 
	 */
	public Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
		return target.getValue(context, rootObject);
	}

	/**
	 * @see org.springframework.expression.Expression#getValue(EvaluationContext, java.lang.Class)
	 * @param context the context in which to evaluate the expression
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) throws EvaluationException {
		return target.getValue(context, desiredResultType);
	}

	/**
	 * @see org.springframework.expression.Expression#getValue(EvaluationContext, Object, java.lang.Class)
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType) throws EvaluationException {
		return target.getValue(context, rootObject, desiredResultType);
	}

	/**
	 * @see org.springframework.expression.Expression#getValueType()
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public Class<?> getValueType() throws EvaluationException {
		return target.getValueType();
	}

	/**
	 * @see org.springframework.expression.Expression#getValueType(Object)
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public Class<?> getValueType(Object rootObject) throws EvaluationException {
		return target.getValueType(rootObject);
	}

	/**
	 * @see org.springframework.expression.Expression#getValueType(EvaluationContext)
	 * @param context the context in which to evaluate the expression
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public Class<?> getValueType(EvaluationContext context) throws EvaluationException {
		return target.getValueType(context);
	}

	/**
	 * @see org.springframework.expression.Expression#getValueType(EvaluationContext, Object)
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
		return target.getValueType(context, rootObject);
	}

	/**
	 * @see org.springframework.expression.Expression#getValueTypeDescriptor()
	 * @return a type descriptor for values that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
		return target.getValueTypeDescriptor();
	}

	/**
	 * @see org.springframework.expression.Expression#getValueTypeDescriptor(Object)
	 * @param rootObject the root object against which to evaluate the expression
	 * @return a type descriptor for values that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
		return target.getValueTypeDescriptor(rootObject);
	}

	/**
	 * @see org.springframework.expression.Expression#getValueTypeDescriptor(EvaluationContext)
	 * @param context the context in which to evaluate the expression
	 * @return a type descriptor for values that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
		return target.getValueTypeDescriptor(context);
	}

	/**
	 * @see org.springframework.expression.Expression#getValueTypeDescriptor(EvaluationContext, Object)
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @return a type descriptor for values that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException {
		return target.getValueTypeDescriptor(context, rootObject);
	}

	/**
	 * @see org.springframework.expression.Expression#isWritable(EvaluationContext)
	 * @param context the context in which the expression should be checked
	 * @return {@code true} if the expression is writable; {@code false} otherwise
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		return target.isWritable(context);
	}

	/**
	 * @see org.springframework.expression.Expression#isWritable(EvaluationContext, Object)
	 * @param context the context in which the expression should be checked
	 * @param rootObject the root object against which to evaluate the expression
	 * @return {@code true} if the expression is writable; {@code false} otherwise
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	public boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException {
		return target.isWritable(context, rootObject);
	}

	/**
	 * @see org.springframework.expression.Expression#isWritable(Object)
	 * @param rootObject the root object against which to evaluate the expression
	 * @return {@code true} if the expression is writable; {@code false} otherwise
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	public boolean isWritable(Object rootObject) throws EvaluationException {
		return target.isWritable(rootObject);
	}

	/**
	 * @see org.springframework.expression.Expression#setValue(EvaluationContext, Object)
	 * @param context the context in which to set the value of the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		target.setValue(context, value);
	}

	/**
	 * @see org.springframework.expression.Expression#setValue(Object, Object)
	 * @param rootObject the root object against which to evaluate the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public void setValue(Object rootObject, Object value) throws EvaluationException {
		target.setValue(rootObject, value);
	}

	/**
	 * @see org.springframework.expression.Expression#setValue(EvaluationContext, Object, Object)
	 * @param context the context in which to set the value of the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException {
		target.setValue(context, rootObject, value);
	}

	/**
	 * @see org.springframework.expression.Expression#getExpressionString()
	 * @return the original expression string
	 */
	public String getExpressionString() {
		return target.getExpressionString();
	}
	
	/**
	 * @return String representation for this {@link WrappedExpression}
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"("+getExpressionString()+")";
	}

}
