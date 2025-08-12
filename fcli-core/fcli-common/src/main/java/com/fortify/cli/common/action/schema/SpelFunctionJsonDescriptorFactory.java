package com.fortify.cli.common.action.schema;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.annotations.MethodDescriptor;
import com.fortify.cli.common.action.schema.annotations.ParamDescriptor;
import com.fortify.cli.common.action.schema.annotations.ReturnDescriptor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public final class SpelFunctionJsonDescriptorFactory {
	private static final Logger LOG = LoggerFactory.getLogger(SpelFunctionJsonDescriptorFactory.class);

	public static final SpELFunctionDescriptor getSpelFunctionsDescriptor() {
		return new SpELFunctionDescriptor();
	}

	@Data
	@Reflectable
	public static final class SpELFunctionDescriptor {
		@JsonIgnore
		private final List<SpELFunctionJsonClassDescriptor> classes = new ArrayList<>();
		private final List<SpELFunctionJsonMethodDescriptor> methods = new ArrayList<>();
		private static Set<String> spelFunctionClasses;

		static {
			spelFunctionClasses = Set.of("com.fortify.cli.common.spring.expression.SpelFunctionsStandard",
					"com.fortify.cli.common.action.runner.ActionSpelFunctions",
					"com.fortify.cli.fod.action.helper.FoDActionSpelFunctions",
					"com.fortify.cli.common.action.runner.ActionRunnerContext$ActionUtil",
					"com.fortify.cli.ssc.action.helper.SSCActionSpelFunctions",
					"com.fortify.cli.common.util.StringHelper");
		}

		public SpELFunctionDescriptor() {
			Set<Class<?>> spelFuncClasses = getSpelFunctionClasses();
			collectSpELFunctions(spelFuncClasses);
			collectFunctions();
			methods.sort((a, b) -> a.getName().compareTo(b.name));
		}

		private void collectFunctions() {
			for (SpELFunctionJsonClassDescriptor spELFunctionJsonClassDescriptor : classes) {
				methods.addAll(spELFunctionJsonClassDescriptor.getMethods());
			}
		}

		@JsonIgnore
		public final JsonNode asJson() {
			var result = JsonHelper.getObjectMapper().valueToTree(this);
			if (LOG.isDebugEnabled()) {
				LOG.trace(result.toPrettyString());
			}
			return result;
		}

		/**
		 * This method looks for the methods of each spel class and creates list of
		 * {@link SpELFunctionJsonMethodDescriptor}
		 * 
		 * @param spelFuncClasses {@link }
		 */
		private void collectSpELFunctions(Set<Class<?>> spelFuncClasses) {
			for (Class<?> spelFuncClass : spelFuncClasses) {
				SpELFunctionJsonClassDescriptor classDescriptor = SpELFunctionJsonClassDescriptor.builder()
						.name(spelFuncClass.getSimpleName()).methods(new ArrayList<>()).build();
				for (Method method : spelFuncClass.getDeclaredMethods()) {
					if (!Modifier.isPrivate(method.getModifiers())) {
						classDescriptor.getMethods().add(createMethod(method, spelFuncClass));
					}
				}
				classes.add(classDescriptor);
			}
		}

		private static SpELFunctionJsonMethodDescriptor createMethod(Method method, Class<?> spelFuncClass) {
			MethodDescriptor methodAnnotation = method.getAnnotation(MethodDescriptor.class);
			SpELFunctionJsonMethodDescriptor methodDesc = SpELFunctionJsonMethodDescriptor.builder()
					.name(getPrefix(spelFuncClass) + method.getName())
					.description(methodAnnotation != null ? methodAnnotation.value() : "").params(new ArrayList<>())
					.returns(null).signature("").build();
			Parameter[] params = method.getParameters();
			for (Parameter parameter : params) {
				methodDesc.getParams().add(createParam(parameter));
			}
			ReturnDescriptor returnAnnotation = method.getAnnotation(ReturnDescriptor.class);
			methodDesc.setReturns(createReturnType(method.getReturnType(), returnAnnotation));
			methodDesc.setSignature(constructMethodSignature(method, methodDesc, spelFuncClass));
			return methodDesc;
		}

		private static String constructMethodSignature(Method method, SpELFunctionJsonMethodDescriptor methodDesc,
				Class<?> spelFuncClass) {
			if (Objects.isNull(method)) {
				return "";
			}
			return String.format("%s #%s(%s)", methodDesc.getReturns().getType(),
					spelFuncClass == null ? methodDesc.getName() : getPrefix(spelFuncClass) + methodDesc.getName(),
					getParamsPassed(methodDesc.getParams()));
		}

		private static String getPrefix(Class<?> spelFuncClass) {
			if (spelFuncClass.getSimpleName().equals("FoDActionSpelFunctions")) {
				return "fod.";
			} else if (spelFuncClass.getSimpleName().equals("SSCActionSpelFunctions")) {
				return "ssc.";
			}
			return "";
		}

		private static Object getParamsPassed(List<SpELFunctionJsonParamDescriptor> params) {
			return params.stream().map(p -> p.getType() + " " + p.getName()).collect(Collectors.joining(", "));
		}

		private static SpELFunctionJsonReturnDescriptor createReturnType(Class<?> returnType, ReturnDescriptor returnAnnotation) {
			return SpELFunctionJsonReturnDescriptor.builder().type(getJsonType(returnType))
					.description(returnAnnotation != null ? returnAnnotation.value() : "NA").build();
		}

		private static SpELFunctionJsonParamDescriptor createParam(Parameter parameter) {
			ParamDescriptor paramAnnotation = parameter.getAnnotation(ParamDescriptor.class);
			SpELFunctionJsonParamDescriptor paramDescriptor = SpELFunctionJsonParamDescriptor.builder()
					.name(parameter.getName()).description(paramAnnotation != null ? paramAnnotation.value() : "NA")
					.type(getJsonType(parameter.getType())).build();
			return paramDescriptor;
		}

		public static final String getJsonType(Class<?> clazz) {
			if (String.class.isAssignableFrom(clazz)) {
				return "string";
			}
			if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
				return "boolean";
			}
			if (Number.class.isAssignableFrom(clazz)) {
				return "number";
			} 
			if (Enum.class.isAssignableFrom(clazz)) {
				return "enum";
			}
			if (TemplateExpression.class.isAssignableFrom(clazz)) {
				return "expression";
			}
			if (Map.class.isAssignableFrom(clazz)) {
				return "map";
			}
			if (Collection.class.isAssignableFrom(clazz) || clazz.isArray()) {
				return "array";
			} else {
				return "object";
			}
		}

		@SneakyThrows
		private static Set<Class<?>> getSpelFunctionClasses() {
			Set<Class<?>> spelFuncClasses = spelFunctionClasses.stream().map(id -> {
				try {
					return Class.forName(id);
				} catch (ClassNotFoundException e) {
					LOG.warn(String.format("Class %s does not exist", id));
				}
				return null;
			}).filter(o -> o != null).collect(Collectors.toSet());
			return spelFuncClasses;
		}

	}

	@Data
	@Builder
	@Reflectable
	public static final class SpELFunctionJsonClassDescriptor {
		private final String name;
		private final List<SpELFunctionJsonMethodDescriptor> methods;

	}

	@Data
	@Builder(toBuilder = true)
	@Reflectable
	public static final class SpELFunctionJsonMethodDescriptor {
		private final String name;
		private final String description;
		private String signature;
		private List<SpELFunctionJsonParamDescriptor> params;
		private SpELFunctionJsonReturnDescriptor returns;
	}

	@Data
	@Builder(toBuilder = true)
	@Reflectable
	public static final class SpELFunctionJsonParamDescriptor {
		private final String name;
		private final String type;
		private final String description;
	}

	@Data
	@Builder(toBuilder = true)
	@Reflectable
	public static final class SpELFunctionJsonReturnDescriptor {
		private final String type;
		private final String description;

	}

	public static void main(String[] args) {
		System.out.println(SpelFunctionJsonDescriptorFactory.getSpelFunctionsDescriptor().asJson().toPrettyString());
	}
}
