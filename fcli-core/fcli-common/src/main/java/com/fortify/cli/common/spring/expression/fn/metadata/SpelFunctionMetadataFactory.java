package com.fortify.cli.common.spring.expression.fn.metadata;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionDescription;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionParamDescription;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionPrefix;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionReturnDescription;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.ReflectionHelper;

import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public final class SpelFunctionMetadataFactory {
	private static final Logger LOG = LoggerFactory.getLogger(SpelFunctionMetadataFactory.class);

	public static final SpelFunctionDescriptors getSpelFunctionsDescriptor() {
	    Collection<Class<?>> spelFunctionClazzes = Stream.of(
	            "com.fortify.cli.common.spring.expression.fn.SpelFunctionsStandard",
                "com.fortify.cli.common.action.runner.ActionSpelFunctions",
                "com.fortify.cli.fod.action.helper.FoDActionSpelFunctions",
                "com.fortify.cli.common.action.runner.ActionRunnerContext$ActionUtil",
                "com.fortify.cli.ssc.action.helper.SSCActionSpelFunctions")
	       .map(c->toClass(c))
	       .collect(Collectors.toList());
		return new SpelFunctionDescriptors(spelFunctionClazzes);
	}

	@SneakyThrows
    private static Class<?> toClass(String c) {
        return Class.forName(c);
    }

    @Data
	@Reflectable
	public static final class SpelFunctionDescriptors {
		private final List<SpelFunctionDescriptor> functions;

		public SpelFunctionDescriptors(Collection<Class<?>> spelFunctionClazzes) {
			this.functions = collectSpelFunctions(spelFunctionClazzes);
			functions.sort((a, b) -> a.getName().compareTo(b.name));
		}

		@JsonIgnore
		public final JsonNode asJson() {
			var result = JsonHelper.getObjectMapper().valueToTree(this);
			if (LOG.isDebugEnabled()) {
				LOG.trace(result.toPrettyString());
			}
			return result;
		}

		private static final List<SpelFunctionDescriptor> collectSpelFunctions(Collection<Class<?>> spelFunctionClazzes) {
		    return spelFunctionClazzes.stream()
		        .flatMap(c->createFunctionDescriptorsStream(c))
		        .collect(Collectors.toList());
		}
		
		private static final Stream<SpelFunctionDescriptor> createFunctionDescriptorsStream(Class<?> spelFunctionClazz) {
		    return Stream.of(spelFunctionClazz.getDeclaredMethods())
		            .filter(m->!Modifier.isPrivate(m.getModifiers()))
		            .map(m->createSpelFunctionDescriptor(spelFunctionClazz, m));
		}
		
		private static final SpelFunctionDescriptor createSpelFunctionDescriptor(Class<?> spelFunctionClazz, Method spelFunctionMethod) {
		    var prefix = ReflectionHelper.getAnnotationValue(spelFunctionClazz, SpelFunctionPrefix.class, SpelFunctionPrefix::value, ()->"");
		    var name = prefix + spelFunctionMethod.getName();
		    var desc = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunctionDescription.class, SpelFunctionDescription::value, ()->"");
		    var params = createParamDescriptors(spelFunctionMethod);
		    var returns= createReturnDescriptor(spelFunctionMethod);
		    var signature = createSignature(name, params, returns);
		    return SpelFunctionDescriptor.builder()
					.name(name)
					.description(desc)
					.params(params)
					.returns(returns)
					.signature(signature).build();
		}

        private static String createSignature(String name, List<SpelFunctionParamDescriptor> params, SpelFunctionReturnDescriptor returns) {
		    var paramsString = params.stream()
		            .map(p->String.format("%s %s", p.getType(), p.getName()))
		            .collect(Collectors.joining(", "));
			return String.format("%s #%s(%s)", returns.getType(), name, paramsString);
		}

		private static SpelFunctionReturnDescriptor createReturnDescriptor(Method spelFunctionMethod) {
		    var type = getJsonType(spelFunctionMethod.getReturnType());
		    var desc = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunctionReturnDescription.class, SpelFunctionReturnDescription::value, ()->"N/A");
			return SpelFunctionReturnDescriptor.builder()
			        .type(type)
					.description(desc)
					.build();
		}
		
        private static List<SpelFunctionParamDescriptor> createParamDescriptors(Method spelFunctionMethod) {
           return Stream.of(spelFunctionMethod.getParameters())
                   .map(p->createParamDescriptor(p))
                   .collect(Collectors.toList());
        }

		private static SpelFunctionParamDescriptor createParamDescriptor(Parameter parameter) {
		    var type = getJsonType(parameter.getType());
            var desc = ReflectionHelper.getAnnotationValue(parameter, SpelFunctionParamDescription.class, SpelFunctionParamDescription::value, ()->"N/A");
			return SpelFunctionParamDescriptor.builder()
					.name(parameter.getName())
					.description(desc)
					.type(type)
					.build();
		}

		// TODO Remove duplication with ActionSchemaHelper::getJsonType
		private static final String getJsonType(Class<?> clazz) {
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
	}

	@Data @Builder
	@Reflectable
	public static final class SpelFunctionDescriptor {
	    @JsonIgnore private final Class<?> clazz;
		private final String name;
		private final String description;
		private final String signature;
		private final List<SpelFunctionParamDescriptor> params;
		private final SpelFunctionReturnDescriptor returns;
	}

	@Data @Builder
	@Reflectable
	public static final class SpelFunctionParamDescriptor {
		private final String name;
		private final String type;
		private final String description;
	}

	@Data @Builder
	@Reflectable
	public static final class SpelFunctionReturnDescriptor {
		private final String type;
		private final String description;

	}
	
	public static void main(String[] args) {
        System.out.println(SpelFunctionMetadataFactory.getSpelFunctionsDescriptor().asJson().toPrettyString());
    }
}
