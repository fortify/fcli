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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.util.StringUtils;

/**
 * This is a helper class to create a documentation of the FCLI Actions in ASCII
 * Doc format
 * 
 * @author svijaykumar
 */
public class FcliActionsDocCreator {

	private final Map<Class<?>, Class<?>> parentChildMapping = new HashMap<>();
	private final Map<String, String> classNameToVarNameMapping = new HashMap<>();
	private final Map<String, Class<?>> classCache = new HashMap<>();
	private final Map<Class<?>, Boolean> classToParentAccess = new HashMap<Class<?>, Boolean>();
	private Set<Class<?>> enums = new HashSet<Class<?>>();
	private Map<Class<?>, Set<FcliActionsModel>> model = new LinkedHashMap<>();
	private final String startTable = "|===";
	private static final String NEXT_LINE = "\n";
	private final String endTable = startTable + NEXT_LINE + NEXT_LINE;
	private final String columnsWidth = "2,2,6";
	private final String classSeparator = "::";
	private final String hyperlinkBegin = "<<";
	private final String hyperlinkEnd = ">>";
	private final String instructionsTableHeaders = "|Name |Type |Description";
	private final String SECTION_PREFIX = "== ";
	private Set<String> setOfActionChildren = new HashSet<>();
	private Set<String> setOfActionStepChildren = new HashSet<>();

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
			creator.readFieldsRecursively(Action.class, null);
			boolean success = creator.generateSchemaDoc(outputFilePath, outputVersion);
			if (!success) {
				System.err.println("Failed to generate schema documentation.");
			}
		}
	}

	private boolean generateSchemaDoc(Path outputFilePath, String outputVersion)
			throws IOException, ClassNotFoundException {
		Files.createDirectories(outputFilePath);
		var outputFile = outputFilePath.resolve(String.format("fcli-action-doc-%s.adoc", outputVersion));
		StringBuilder builder = new StringBuilder();
		String topLevelTable = createTopLevelTable();
		builder.append(topLevelTable);
		for (Map.Entry<Class<?>, Set<FcliActionsModel>> entry : model.entrySet()) {
			if (entry.getKey().getSimpleName().equals("Action")) {
				continue;
			}
			if (entry.getKey().getSimpleName().equals("ActionStep")) {
				String otherTable = createTableFor(entry);
				builder.append(otherTable);
			} else if (setOfActionChildren.contains(entry.getKey().getSimpleName())) {
				String otherTable = createOtherTables(entry);
				builder.append(otherTable);
			} else if (setOfActionStepChildren.contains(entry.getKey().getSimpleName())) {
				String otherTable = createOtherTables(entry);
				builder.append(otherTable);
			}
		}

		List<String> additionalTables = Arrays.asList("steps::out.write::", "steps::writer.append::");
		Class<?> formatterClass = Class.forName("com.fortify.cli.common.action.model.TemplateExpressionWithFormatter");
		for (String prefix : additionalTables) {
			String otherTable = createTablesFor(formatterClass, prefix);
			builder.append(otherTable);
		}

		for (Class<?> enumObj : enums) {
			String enumName = enumObj.getSimpleName();
			builder.append(NEXT_LINE);
			builder.append("== " + getEnumNameFor(enumName)).append(NEXT_LINE);
			builder.append(String.format("[[%s]]", enumName.toLowerCase())).append(NEXT_LINE);
			builder.append("[options=\"header\"]").append(NEXT_LINE);
			builder.append(startTable).append(NEXT_LINE);
			builder.append("|Value |Description").append(NEXT_LINE);

			Field[] fields = enumObj.getFields();
			for (Field field : fields) {
				Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
				for (Annotation annotation : declaredAnnotations) {
					String enumItem = field.getName();
					String enumDescription = "";
					if (annotation instanceof JsonPropertyDescription jsonDescription) {
						enumDescription = jsonDescription.value();
					}
					builder.append("|" + enumItem).append(NEXT_LINE);
					builder.append("|" + enumDescription).append(NEXT_LINE);
				}
			}
			builder.append(endTable).append(NEXT_LINE).append(NEXT_LINE);
		}

		Files.writeString(outputFile, builder.toString(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING);

		System.out.println("AsciiDoc file generated at: " + outputFilePath);
		return true;
	}

	private String createTablesFor(Class<?> classObj, String prefix) {
		StringBuilder builder = new StringBuilder();
		Set<FcliActionsModel> clazzFields = model.get(classObj);
		if (prefix.isBlank()) {
			return "";
		}
		String tableHeaderContent = prepareTableHeader(clazzFields, classObj, prefix);
		builder.append(tableHeaderContent);
		builder.append(prepareTableDepthData(clazzFields, prefix));
		builder.append(endTable);
		return builder.toString();
	}

	private String createTableFor(Entry<Class<?>, Set<FcliActionsModel>> entry) {
		StringBuilder builder = new StringBuilder();
		Class<?> clazzObject = entry.getKey();
		Set<FcliActionsModel> clazzFields = entry.getValue();
		String prefix = getPrefixFor(clazzObject);
		if (prefix.isBlank()) {
			return "";
		}
		String tableHeaderContent = prepareTableHeader(clazzFields, clazzObject, prefix);
		builder.append(tableHeaderContent);
		builder.append(prepareTableData(clazzFields, prefix));
		builder.append(endTable);
		return builder.toString();
	}

	private String createOtherTables(Entry<Class<?>, Set<FcliActionsModel>> entry) {
		StringBuilder builder = new StringBuilder();
		Class<?> clazzObject = entry.getKey();
		Set<FcliActionsModel> clazzFields = entry.getValue();
		String prefix = getPrefixFor(clazzObject);
		if (prefix.isBlank()) {
			return "";
		}
		String tableHeaderContent = prepareTableHeader(clazzFields, clazzObject, prefix);
		builder.append(tableHeaderContent);
		builder.append(prepareTableDepthData(clazzFields, prefix));
		builder.append(endTable);
		return builder.toString();
	}

	private String createTopLevelTable() {
		StringBuilder builder = new StringBuilder();
		Set<FcliActionsModel> actionClassAttributes = model.entrySet().stream()
				.filter(obj -> obj.getKey().getSimpleName().equals("Action")).map(Map.Entry::getValue).findFirst()
				.orElse(null);

		builder.append(createTopLevelHeader());

		for (FcliActionsModel fcliActionsModel : actionClassAttributes) {
			Class<?> actionType = fcliActionsModel.getActionType();
			String actionTypeStr = fcliActionsModel.getActionTypeStr();
			boolean isCustomClassReference = isFcliModelClass(actionType);
			boolean collectionOfCustomClass = isCollectionOfCustomClass(actionTypeStr, actionType);

			/** Name Row */
			if (isCustomClassReference) {
				builder.append("|").append(hyperlinkBegin);
				builder.append(fcliActionsModel.getActionName());
				builder.append(", ");
				builder.append(fcliActionsModel.getActionName());
				builder.append(hyperlinkEnd);
			} else {
				builder.append("|").append(fcliActionsModel.getActionName());
			}
			builder.append(NEXT_LINE);

			/** Type Row */
			if (fcliActionsModel.getActionType().isEnum()) {
				builder.append("|").append(hyperlinkBegin);
				builder.append(fcliActionsModel.getActionType().getSimpleName().toLowerCase());
				builder.append(", ");
				builder.append(getTypeCellData(actionTypeStr));
				builder.append(hyperlinkEnd);
			} else if (collectionOfCustomClass) {
				builder.append("|").append(hyperlinkBegin);
				builder.append(getTableNameForClass(fcliActionsModel.getActionTypeStr()));
				builder.append(", ");
				builder.append(getTypeCellData(actionTypeStr));
				builder.append(hyperlinkEnd);
			} else {
				builder.append("|").append(getTypeCellData(actionTypeStr));
			}
			builder.append(NEXT_LINE);

			/** Description Row */
			builder.append("|").append(fcliActionsModel.getActionDescription()).append(NEXT_LINE);
		}
		builder.append(endTable);
		return builder.toString();
	}

	private String createTopLevelHeader() {
		StringBuilder builder = new StringBuilder();
		builder.append("[[top-level-instructions]]").append(NEXT_LINE);
		builder.append("[options=\"header\"]").append(NEXT_LINE).append(NEXT_LINE);
		builder.append(String.format("[cols=\"%s\"]", "4,4,10")).append(NEXT_LINE);
		builder.append(SECTION_PREFIX + "Top Level Instructions").append(NEXT_LINE);
		builder.append(startTable).append(NEXT_LINE);
		builder.append(instructionsTableHeaders).append(NEXT_LINE).append(NEXT_LINE);
		return builder.toString();
	}

	private boolean isCollectionOfCustomClass(String actionTypeStr, Class<?> actionType) {
		boolean isCollectionObject = Map.class.isAssignableFrom(actionType) || List.class.isAssignableFrom(actionType);
		if (isCollectionObject) {
			String substring = actionTypeStr.substring(actionTypeStr.indexOf('<') + 1, actionTypeStr.indexOf('>'));
			String[] splits = substring.split(",");
			for (String split : splits) {
				if (split.contains("com.fortify.cli.common.action.model.")) {
					return true;
				}
			}
		}
		return false;
	}

	private String prepareTableDepthData(Set<FcliActionsModel> set, String prefix) {
		StringBuilder builder = new StringBuilder();
		for (FcliActionsModel modelEntry : set) {
			Class<?> actionType = modelEntry.getActionType();
			boolean isCustomClassReference = isFcliModelClass(actionType);
			boolean isCollectionObject = Map.class.isAssignableFrom(actionType)
					|| List.class.isAssignableFrom(actionType);
			String actionTypeStr = modelEntry.getActionTypeStr();
			boolean isCollectionOfCustomClass = false;
			if (isCollectionObject) {
				String substring = actionTypeStr.substring(actionTypeStr.indexOf('<') + 1, actionTypeStr.indexOf('>'));
				String[] splits = substring.split(",");
				for (String split : splits) {
					if (split.trim().contains("com.fortify.cli.common.action.model.")) {
						isCollectionOfCustomClass = true;
						break;
					}
				}
			}

			builder.append("|");
			builder.append((prefix.isEmpty() ? "" : prefix) + modelEntry.getActionName());
			builder.append(NEXT_LINE);

			if (modelEntry.getActionType().isEnum()) {
				builder.append("|");
				builder.append(
						String.format(hyperlinkBegin + "%s", modelEntry.getActionType().getSimpleName().toLowerCase()));
				builder.append(", ");
				builder.append(getTypeCellData(actionTypeStr));
				builder.append(hyperlinkEnd);
				builder.append(NEXT_LINE);
			} else if (isCollectionOfCustomClass
					&& modelEntry.getActionTypeStr().contains("com.fortify.cli.common.action.model.")) {
				builder.append("|");
				builder.append(hyperlinkBegin + getTableNameForClass(modelEntry.getActionTypeStr()));
				builder.append(", ");
				builder.append(getTypeCellData(actionTypeStr));
				builder.append(hyperlinkEnd);
				builder.append(NEXT_LINE);
			} else {
				builder.append("|").append(getTypeCellData(actionTypeStr)).append(NEXT_LINE);
			}
			builder.append("|").append(modelEntry.getActionDescription()).append(NEXT_LINE);

			if (isCustomClassReference) {
				if (!Objects.isNull(model.get(actionType))) {
					Set<FcliActionsModel> entrySet = model.get(actionType);
					String tableData = prepareTableData(entrySet, prefix + modelEntry.getActionName() + "::");
					builder.append(tableData);
				}
			}

			if (isCollectionOfCustomClass) {
				String actionTypes = modelEntry.getActionTypeStr().substring(
						modelEntry.getActionTypeStr().indexOf('<') + 1, modelEntry.getActionTypeStr().indexOf('>'));
				String[] splits = actionTypes.split(",");
				for (String split : splits) {
					Class<?> classObject = classCache.get(split.trim());
					if (!Objects.isNull(classObject) && !Objects.isNull(model.get(classObject))
							&& !classObject.getSimpleName().equals("ActionStep")) {
						Set<FcliActionsModel> entrySet = model.get(classObject);
						String tableData = prepareTableData(entrySet, prefix + modelEntry.getActionName() + "::");
						builder.append(tableData);
					}
				}
			}
		}
		return builder.toString();
	}

	private String getTableNameForClass(String classFQName) {
		String substring = classFQName.substring(classFQName.indexOf('<') + 1, classFQName.indexOf('>'));
		String[] splits = substring.split(",");
		for (String split : splits) {
			switch (split.trim()) {
			case "com.fortify.cli.common.action.model.ActionStep": {
				return "steps";
			}
			case "com.fortify.cli.common.action.model.ActionStepRestCallEntry":
				return "rest.call";
			case "com.fortify.cli.common.action.model.ActionCliOption":
				return "cli.options";
			case "com.fortify.cli.common.action.model.ActionStepRestTargetEntry":
				return "rest.target";
			case "com.fortify.cli.common.action.model.ActionStepRunFcliEntry":
				return "run.fcli";
			case "com.fortify.cli.common.action.model.TemplateExpressionWithFormatter":
				return "var.set";
			}
		}
		return "top-level-instructions";
	}

	private String prepareTableHeader(Set<FcliActionsModel> value, Class<?> clazz, String prefix) {
		StringBuilder builder = new StringBuilder();
		if (!value.isEmpty() && !clazz.getSimpleName().equals("Action") && !prefix.isBlank()) {
			String[] splits = prefix.split(classSeparator);
			String tableName = String.format("%s", splits[splits.length - 1]);
			String tocSection = getTocSections(prefix);
			builder.append(String.format("%s%s\n", tocSection, prefix.substring(0, prefix.length() - 2)));
			builder.append(String.format("[[%s]]\n", tableName));
			builder.append(NEXT_LINE);
			builder.append(String.format("[cols=\"%s\"]", columnsWidth));
			builder.append(NEXT_LINE);
			builder.append(startTable).append(NEXT_LINE);
			builder.append(instructionsTableHeaders).append(NEXT_LINE).append(NEXT_LINE);
		}
		return builder.toString();
	}

	private String prepareTableData(Set<FcliActionsModel> entrySet, String prefix) {
		StringBuilder builder = new StringBuilder();
		for (FcliActionsModel modelEntry : entrySet) {
			Class<?> actionType = modelEntry.getActionType();
			boolean isCustomClassReference = isFcliModelClass(actionType);
			boolean isCollectionObject = Map.class.isAssignableFrom(actionType)
					|| List.class.isAssignableFrom(actionType);
			String actionTypeStr = modelEntry.getActionTypeStr();
			boolean isCollectionOfCustomClass = false;
			if (isCollectionObject) {
				String substring = actionTypeStr.substring(actionTypeStr.indexOf('<') + 1, actionTypeStr.indexOf('>'));
				String[] splits = substring.split(",");
				for (String split : splits) {
					if (split.contains("com.fortify.cli.common.action.model.")) {
						isCollectionOfCustomClass = true;
						break;
					}
				}
			}
			if (isCustomClassReference) {
				builder.append("|");
				if (modelEntry.getActionTypeStr().contains("com.fortify.cli.common.action.model.ActionStep>")) {
					builder.append((prefix.isEmpty() ? "" : prefix) + modelEntry.getActionName());
				} else {
					builder.append(String.format(hyperlinkBegin + "%s", modelEntry.getActionName()));
					builder.append(", ");
					builder.append((prefix.isEmpty() ? "" : prefix) + modelEntry.getActionName());
					builder.append(hyperlinkEnd);
				}
				builder.append(NEXT_LINE);
			} else if (isCollectionOfCustomClass) {
				builder.append("|");
				if (modelEntry.getActionTypeStr().contains("com.fortify.cli.common.action.model.ActionStep>")) {
					builder.append((prefix.isEmpty() ? "" : prefix) + modelEntry.getActionName());
				} else {
					builder.append(String.format(hyperlinkBegin + "%s", modelEntry.getActionName()));
					builder.append(", ");
					builder.append((prefix.isEmpty() ? "" : prefix) + modelEntry.getActionName());
					builder.append(hyperlinkEnd);
				}
				builder.append(NEXT_LINE);
			} else {
				builder.append("|").append(prefix.isEmpty() ? "" : prefix);
				builder.append(String.format("%s", modelEntry.getActionName())).append(NEXT_LINE);
			}
			if (modelEntry.getActionType().isEnum()) {
				builder.append("|");
				builder.append(
						String.format(hyperlinkBegin + "%s", modelEntry.getActionType().getSimpleName().toLowerCase()));
				builder.append(", ");
				builder.append(getTypeCellData(actionTypeStr));
				builder.append(hyperlinkEnd);
				builder.append(NEXT_LINE);
			} else if (isCollectionOfCustomClass
					&& modelEntry.getActionTypeStr().contains("com.fortify.cli.common.action.model.ActionStep>")) {
				builder.append("|");
				builder.append(hyperlinkBegin + getTableNameForClass(modelEntry.getActionTypeStr()));
				builder.append(", ");
				builder.append(getTypeCellData(actionTypeStr));
				builder.append(hyperlinkEnd);
				builder.append(NEXT_LINE);
			} else {
				builder.append("|").append(getTypeCellData(actionTypeStr)).append(NEXT_LINE);
			}
			builder.append("|").append(modelEntry.getActionDescription()).append(NEXT_LINE);
		}
		return builder.toString();
	}

	private String getTocSections(String prefix) {
		String[] subsections = prefix.split("::");
		String tocSection = "=== ";
		if (subsections.length == 2) {
			tocSection = "==== ";
		} else if (subsections.length == 3) {
			tocSection = "===== ";
		}
		return tocSection;
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

	private String getTypeCellData(String typeStr) {
		if (typeStr == null || typeStr.isEmpty()) {
			return "";
		}
		String cellData = getEnumNameFor(typeStr);
		if (cellData != null) {
			return cellData;
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
			return containerType + "<" + String.join(", ", processedParts) + ">";
		} else if (typeStr.equals("TemplateExpression")) {
			return "expression";
		}
		return typeStr.contains("com.fortify.cli.common.action.model.") ? "object" : typeStr.toLowerCase();
	}

	private String getEnumNameFor(String typeStr) {
		switch (typeStr) {
		case "LogSensitivityLevel": {
			return "sensitivity level";
		}
		case "OutputType": {
			return "output type";
		}
		case "CheckStatus": {
			return "check status";
		}
		case "ActionConfigOutput": {
			return "configuration output";
		}
		case "ActionStepRequestType": {
			return "step request type";
		}
		case "SignatureStatus": {
			return "signature status";
		}
		case "PublicKeySource": {
			return "public key source";
		}
		}
		return null;
	}

	private String jsonLikeType(String javaType) {
		if (javaType.contains("Map")) {
			return "map";
		} else if (javaType.contains("List")) {
			return "array";
		}
		return "";
	}

	private void readFieldsRecursively(Class<?> clazz, Class<?> baseClazz) throws ClassNotFoundException {
		if (clazz == null || model.containsKey(clazz)) {
			return;
		}

		Set<FcliActionsModel> children = new LinkedHashSet<>();
		if (isFcliModelClass(clazz)) {
			model.putIfAbsent(clazz, children);

		}

		List<Field> declaredFields = new ArrayList<Field>();
		try {
			declaredFields.addAll(Arrays.asList(clazz.getDeclaredFields()));
			Class<?> superclass = clazz.getSuperclass();
			if (!Objects.isNull(superclass) && isFcliModelClass(superclass)) {
				declaredFields.addAll(Arrays.asList(superclass.getDeclaredFields()));
			}
		} catch (NoClassDefFoundError e) {
			return;
		}

		if (!Objects.isNull(baseClazz)) {
			clazz = baseClazz;
			classToParentAccess.putIfAbsent(baseClazz, true);
		}

		for (Field field : declaredFields) {
			Class<?> fieldType = field.getType();
			if (clazz.getSimpleName().equals("Action") && isFcliModelClass(fieldType)) {
				setOfActionChildren.add(fieldType.getSimpleName());
			} else if (clazz.getSimpleName().equals("ActionStep") && isFcliModelClass(fieldType)) {
				setOfActionStepChildren.add(fieldType.getSimpleName());
			}
			if (!fieldType.isPrimitive() && !fieldType.getName().startsWith("java.") && !fieldType.isEnum()) {
				readFieldsRecursively(fieldType, null);
			}

			if (fieldType.getName().startsWith("com.fortify.cli.") && clazz.getName().startsWith("com.fortify.cli.")
					&& !fieldType.isEnum()) {
				if (!fieldType.equals(clazz)) {
					updateChildParentRelation(fieldType, clazz);
				}
			} else if (fieldType.getName().startsWith("com.fortify.cli.")
					&& clazz.getName().startsWith("com.fortify.cli.") && fieldType.isEnum()) {
				enums.add(fieldType);
			}

			String fieldTypeStr = fieldType.getSimpleName();
			Annotation[] declaredAnnotations = field.getDeclaredAnnotations();

			String fieldName = null;
			String fieldDescription = null;

			for (Annotation annotation : declaredAnnotations) {
				if (annotation instanceof JsonProperty jsonProperty) {
					fieldName = jsonProperty.value();
				}
				if (annotation instanceof JsonPropertyDescription jsonDescription) {
					fieldDescription = jsonDescription.value();
				}
			}

			if (!Objects.isNull(fieldName) && !fieldName.isBlank() && !Objects.isNull(fieldDescription)
					&& !fieldDescription.isBlank()) {
				classNameToVarNameMapping.putIfAbsent(fieldType.getSimpleName(), fieldName);
				if (Map.class.isAssignableFrom(fieldType) || List.class.isAssignableFrom(fieldType)) {
					Type genericType = field.getGenericType();
					if (genericType instanceof ParameterizedType paramType) {
						Type[] actualTypeArgs = paramType.getActualTypeArguments();
						List<String> typeItems = new ArrayList<String>();
						for (Type type : actualTypeArgs) {
							if (type.getTypeName().contains("String")) {
								typeItems.add("id");
							} else if (isFcliModelClass(type)) {
								typeItems.add(type.getTypeName());
								Class<?> child = Class.forName(type.getTypeName());
								if (clazz.getSimpleName().equals("Action")) {
									setOfActionChildren.add(child.getSimpleName());
								} else if (clazz.getSimpleName().equals("ActionStep")) {
									setOfActionStepChildren.add(child.getSimpleName());
								}
							} else if (Class.forName(type.getTypeName()).isEnum()) {
								typeItems.add(getEnumNameFor(Class.forName(type.getTypeName()).getSimpleName()));
							} else if (type.getTypeName().contains("TemplateExpression")) {
								typeItems.add("expression");
							}

						}
						String types = typeItems.stream().collect(Collectors.joining(", "));
						fieldTypeStr = fieldTypeStr + "<" + types + ">";
						FcliActionsModel child = new FcliActionsModel(fieldName, fieldType, fieldTypeStr,
								fieldDescription);
						children.add(child);

						for (Type typeArg : actualTypeArgs) {
							if (isFcliModelClass(typeArg)) {
								Class<?> argClass = getClassForName(typeArg.getTypeName());
								if (argClass != null && !argClass.equals(clazz)) {
									updateChildParentRelation(argClass, clazz);
									classNameToVarNameMapping.putIfAbsent(argClass.getSimpleName(), fieldName);
									readFieldsRecursively(argClass, null);
								}
							}
						}
					}
				} else {
					String typeStr = null;
					if (isFcliModelClass(fieldType)) {
						typeStr = fieldType.getTypeName();
					}
					FcliActionsModel child = new FcliActionsModel(fieldName, fieldType, typeStr, fieldDescription);
					children.add(child);
				}
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
			return clazz != null && !clazz.isPrimitive() && !clazz.getName().startsWith("java.") && !clazz.isEnum()
					&& !clazz.getSimpleName().equals("TemplateExpression");
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isFcliModelClass(Type typeArg) {
		return isCustomClass(typeArg) && typeArg.getTypeName().startsWith("com.fortify.cli.common.action.model");
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
