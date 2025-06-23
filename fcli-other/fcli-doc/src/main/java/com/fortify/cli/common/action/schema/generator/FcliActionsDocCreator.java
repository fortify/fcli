package com.fortify.cli.common.action.schema.generator;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.common.variable.FCLIActionPropertyMetaInfo;

/**
 * This is a helper class to create a documentation of the FCLI Actions in ASCII
 * Doc format
 * 
 * @author svijaykumar
 */
public class FcliActionsDocCreator {

	private static final String NEXT_LINE = "\n";
	private final Map<Class<?>, Class<?>> parentChildMapping = new HashMap<>();
	private final Map<String, String> classNameToVarNameMapping = new HashMap<>();
	private final Map<String, Class<?>> classCache = new HashMap<>();

	public static void main(String[] args) throws ClassNotFoundException, Exception {
		if (args.length != 3) {
			throw new IllegalArgumentException(
					"This command must be run as FcliActionsDocCreator <true (dev release)|false (final release)> <action schema version> <schema output file location>");
		}
		var isDevelopmentRelease = args[0];
		var actionSchemaVersion = args[1];
		var outputFilePath = Path.of(args[2]);
		var actionSchemaMajorVersion = StringUtils.substringBefore(actionSchemaVersion, ".");
		var devOutputVersion = String.format("dev-%s.x", actionSchemaMajorVersion);

		var outputVersion = isDevelopmentRelease.equals("true") ? devOutputVersion : actionSchemaVersion;
		if (!devOutputVersion.equals(outputVersion)) {
			System.out.println(
					"Fortify CLI action schema not being generated as " + outputVersion + " schema already exists");
			return;
		} else {

			FcliActionsDocCreator creator = new FcliActionsDocCreator();

			Map<Class<?>, Set<FcliActionsModel>> model = new LinkedHashMap<>();
			creator.readFieldsRecursively(Action.class, model);

			boolean success = creator.generateSchemaDoc(model, outputFilePath, outputVersion);
			if (!success) {
				System.err.println("Failed to generate schema documentation.");
			}
		}
	}

	private boolean generateSchemaDoc(Map<Class<?>, Set<FcliActionsModel>> model, Path outputFilePath,
			String outputVersion) throws IOException {
		Files.createDirectories(outputFilePath);
		var outputFile = outputFilePath.resolve(String.format("fcli-action-doc-%s.adoc", outputVersion));

		StringBuilder builder = new StringBuilder();
		builder.append("= Fortify CLI Actions Schema Documentation").append(NEXT_LINE).append(NEXT_LINE);
		builder.append(".Fortify CLI Actions").append(NEXT_LINE);
		builder.append("|===").append(NEXT_LINE);
		builder.append("|Name |Type |Description").append(NEXT_LINE).append(NEXT_LINE);

		for (Map.Entry<Class<?>, Set<FcliActionsModel>> entry : model.entrySet()) {
			Class<?> clazz = entry.getKey();
			String prefix = getPrefixFor(clazz);
			for (FcliActionsModel modelEntry : entry.getValue()) {
				builder.append("|").append(prefix.isEmpty() ? "" : prefix).append(modelEntry.getActionName())
						.append(NEXT_LINE);
				builder.append("|")
						.append(getType(modelEntry.getActionType() == null ? "" : modelEntry.getActionType()))
						.append(NEXT_LINE);
				builder.append("|").append(modelEntry.getActionDescription()).append(NEXT_LINE);
			}
		}
		builder.append("|===");

		Files.writeString(outputFile, builder.toString(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING);

//		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
//			writer.write(builder.toString());
		System.out.println("AsciiDoc file generated at: " + outputFilePath);
		return true;
//		} catch (IOException e) {
//			e.printStackTrace();
//			return false;
//		}
	}

	private String getPrefixFor(Class<?> clazz) {
		if ("Action".equals(clazz.getSimpleName())) {
			return "";
		}
		List<Class<?>> parents = new ArrayList<>();
		Class<?> current = clazz;
		while (current != null && !"Action".equals(current.getSimpleName())) {
			parents.add(current);
			current = parentChildMapping.get(current);
		}
		Collections.reverse(parents);

		StringBuilder sb = new StringBuilder();
		for (Class<?> parent : parents) {
			String varName = classNameToVarNameMapping.get(parent.getSimpleName());
			if (varName != null) {
				sb.append(varName).append("::");
			}
		}
		return sb.toString();
	}

	private String getType(String typeStr) {
		if (typeStr == null || typeStr.isEmpty()) {
			return "";
		}
		if (typeStr.contains("Map") || typeStr.contains("List")) {
			int startIndex = typeStr.indexOf('<');
			int endIndex = typeStr.indexOf('>');
			if (startIndex < 0 || endIndex < 0 || endIndex <= startIndex) {
				return typeStr.toLowerCase();
			}
			String containerType = jsonLikeType(typeStr.substring(0, startIndex));
			String innerTypes = typeStr.substring(startIndex + 1, endIndex);
			String[] typeParts = innerTypes.split(",");
			List<String> processedParts = new ArrayList<>();

			for (String part : typeParts) {
				part = part.trim();
				if (part.contains("String")) {
					processedParts.add("id");
				} else {
					String[] subParts = part.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
					processedParts.add(subParts[subParts.length - 1].toLowerCase());
				}
			}
			return containerType + "<" + String.join(",", processedParts) + ">";
		}
		return typeStr.toLowerCase();
	}

	private String jsonLikeType(String javaType) {
		if (javaType.contains("Map")) {
			return "map";
		} else if (javaType.contains("List")) {
			return "array";
		}
		return "";
	}

	private void readFieldsRecursively(Class<?> clazz, Map<Class<?>, Set<FcliActionsModel>> model) {
		if (clazz == null || model.containsKey(clazz)) {
			return;
		}

		Set<FcliActionsModel> children = new LinkedHashSet<>();
		Field[] declaredFields;

		try {
			declaredFields = clazz.getDeclaredFields();
		} catch (NoClassDefFoundError e) {
			return;
		}

		model.putIfAbsent(clazz, children);

		for (Field field : declaredFields) {
			Class<?> fieldType = field.getType();
			String fieldName = field.getName();

			if (fieldType.getName().startsWith("com.fortify.cli.") && clazz.getName().startsWith("com.fortify.cli.")
					&& !fieldType.isEnum()) {
				if (!fieldType.equals(clazz)) {
					updateChildParentRelation(fieldType, clazz);
				}
				classNameToVarNameMapping.putIfAbsent(fieldType.getSimpleName(), fieldName);
			}

			String fieldTypeStr = fieldType.getSimpleName();
			Annotation[] annotations = field.getAnnotations();

			for (Annotation annotation : annotations) {
				if (annotation instanceof FCLIActionPropertyMetaInfo metaInfo) {
					String metaFieldName = metaInfo.fieldName();
					String metaFieldDesc = metaInfo.fieldDesc();

					if (Map.class.isAssignableFrom(fieldType) || List.class.isAssignableFrom(fieldType)) {
						Type genericType = field.getGenericType();
						if (genericType instanceof ParameterizedType paramType) {
							Type[] actualTypeArgs = paramType.getActualTypeArguments();
							String types = Arrays.stream(actualTypeArgs).map(Type::getTypeName)
									.collect(Collectors.joining(", "));
							fieldTypeStr = fieldTypeStr + "<" + types + ">";
							FcliActionsModel child = new FcliActionsModel(metaFieldName, fieldTypeStr, metaFieldDesc);
							children.add(child);

							for (Type typeArg : actualTypeArgs) {
								if (isCustomClass(typeArg)) {
									Class<?> argClass = getClassForName(typeArg.getTypeName());
									if (argClass != null && !argClass.equals(clazz)) {
										updateChildParentRelation(argClass, clazz);
										classNameToVarNameMapping.putIfAbsent(argClass.getSimpleName(), fieldName);
										readFieldsRecursively(argClass, model);
									}
								}
							}
						}
					} else {
						FcliActionsModel child;
						if ("java.lang.String".equals(fieldType.getName())) {
							child = new FcliActionsModel(metaFieldName, "string", metaFieldDesc);
						} else {
							child = new FcliActionsModel(metaFieldName, fieldName, metaFieldDesc);
						}
						children.add(child);
					}
				}
			}
		}

		// Read superclass fields once after processing current class fields
		Class<?> superClass = clazz.getSuperclass();
		if (superClass != null) {
			readFieldsRecursively(superClass, model);
		}

		// For non-primitive, non-java, non-enum fields, recurse
		for (Field field : declaredFields) {
			Class<?> fieldType = field.getType();
			if (!fieldType.isPrimitive() && !fieldType.getName().startsWith("java.") && !fieldType.isEnum()) {
				readFieldsRecursively(fieldType, model);
			}
		}
	}

	private void updateChildParentRelation(Class<?> childClass, Class<?> parentClass) {
		if (!parentChildMapping.containsKey(childClass)) {
			parentChildMapping.put(childClass, parentClass);
		}
	}

	private boolean isCustomClass(Type typeArg) {
		try {
			Class<?> clazz = getClassForName(typeArg.getTypeName());
			return clazz != null && !clazz.isPrimitive() && !clazz.getName().startsWith("java.") && !clazz.isEnum();
		} catch (Exception e) {
			return false;
		}
	}

	private Class<?> getClassForName(String className) {
		return classCache.computeIfAbsent(className, key -> {
			try {
				return Class.forName(key);
			} catch (ClassNotFoundException e) {
				return null;
			}
		});
	}
}
