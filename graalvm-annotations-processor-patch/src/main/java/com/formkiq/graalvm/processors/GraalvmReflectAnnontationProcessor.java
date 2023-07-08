/**
 * Copyright [2020] FormKiQ Inc. Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a copy of the License
 * at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.formkiq.graalvm.processors;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableClass;
import com.formkiq.graalvm.annotations.ReflectableClasses;
import com.formkiq.graalvm.annotations.ReflectableField;
import com.formkiq.graalvm.annotations.ReflectableImport;
import com.formkiq.graalvm.annotations.ReflectableMethod;
import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Processes {@link Reflectable} {@link ReflectableImport} Annotations. */
@AutoService(Processor.class)
@SupportedAnnotationTypes({
  "com.formkiq.graalvm.annotations.Reflectable",
  "com.formkiq.graalvm.annotations.ReflectableImport",
  "com.formkiq.graalvm.annotations.ReflectableClasses",
  "com.formkiq.graalvm.annotations.ReflectableClass",
  "com.formkiq.graalvm.annotations.ReflectableClass.ReflectableClasses"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class GraalvmReflectAnnontationProcessor extends AbstractProcessor {

  /** The inner class separator character: '$'. */
  private static final char INNER_CLASS_SEPARATOR = '$';
  /** {@link Logger}. */
  private static final Logger LOGGER =
      Logger.getLogger(GraalvmReflectAnnontationProcessor.class.getName());
  /** Log {@link Level}. */
  private static final Level LOGLEVEL = Level.INFO;
  /** The package separator character: '.'. */
  private static final char PACKAGE_SEPARATOR = '.';
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  /** {@link List} of {@link Reflect}. */
  private Map<String, Reflect> reflects = new HashMap<>();

  private TypeElement asTypeElement(final TypeMirror typeMirror) {
    Types typeUtils = this.processingEnv.getTypeUtils();
    return (TypeElement) typeUtils.asElement(typeMirror);
  }

  /**
   * Find Class Names using {@link AnnotationMirror}.
   *
   * @param element {@link Element}
   * @param key {@link String}
   * @return {@link List} {@link String}
   */
  private List<String> findClasses(final Element element, final String key) {

    List<String> classNames = new ArrayList<>();

    List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();

    for (AnnotationMirror annotationMirror : annotationMirrors) {

      Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
          annotationMirror.getElementValues();

      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
          elementValues.entrySet()) {

        String simpleNameKey = entry.getKey().getSimpleName().toString();
        Object value = entry.getValue().getValue();

        if (key.equals(simpleNameKey)) {

          @SuppressWarnings("unchecked")
          List<? extends AnnotationValue> typeMirrors = (List<? extends AnnotationValue>) value;

          for (AnnotationValue val : typeMirrors) {
            String clazz = ((TypeMirror) val.getValue()).toString();

            LOGGER.log(LOGLEVEL, "processing ImportedClass " + clazz);
            processImportedClass(clazz);
          }
        }
      }
    }

    return classNames;
  }

  /**
   * Replacement for {@code Class.forName()} that also returns Class instances. Furthermore, it is
   * also capable of resolving inner class names in Java source
   *
   * @param name the name of the Class
   * @return Class instance for the supplied name
   * @throws ClassNotFoundException if the class was not found
   */
  private Class<?> forName(final String name) throws ClassNotFoundException {

    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {

      int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
      if (lastDotIndex != -1) {
        String innerClassName =
            name.substring(0, lastDotIndex)
                + INNER_CLASS_SEPARATOR
                + name.substring(lastDotIndex + 1);
        return forName(innerClassName);
      }

      throw e;
    }
  }

  String generateReflectConfigPath(final Set<String> keys) {

    Set<String> strings =
        keys.stream()
            .map(m -> removePartsContainingDotFollowedByCapital(m))
            .filter(m -> m != null && m.length() > 1)
            .collect(Collectors.toSet());

    if (strings.isEmpty()) {
      strings.add("default");
    }
    final int shortestLength = strings.stream().mapToInt(String::length).min().getAsInt();

    List<String> list =
        strings.stream()
            .filter(s -> s.length() == shortestLength)
            .sorted()
            .collect(Collectors.toList());

    return list.get(0);
  }

  /**
   * Get ClassName from {@link Element}.
   *
   * @param element {@link Element}
   * @return {@link String}s
   */
  private String getClassName(final Element element) {
    String className = null;

    switch (element.getKind()) {
      case FIELD:
      case CONSTRUCTOR:
      case METHOD:
        className = getClassName(element.getEnclosingElement());
        break;
      case ENUM:
      case CLASS:
        className = getEnclosingPrefix(element.getEnclosingElement())
                +((TypeElement) element).getSimpleName().toString();
        break;
      default:
        break;
    }
    return className;
  }
  
  private String getEnclosingPrefix(final Element element) {
      String prefix = "";
      
      switch (element.getKind()) {
        case PACKAGE:
          prefix = ((PackageElement)element).getQualifiedName().toString() + ".";
          break;
        case ENUM:
        case CLASS:
            prefix = getEnclosingPrefix(element.getEnclosingElement()) + 
                    ((TypeElement)element).getSimpleName().toString() + "$";
          break;
        default:
          break;
    }
    return prefix;
  }

  /**
   * Get {@link Reflect}.
   *
   * @param className {@link String}
   * @return {@link Reflect}
   */
  private Reflect getReflect(final String className) {

    Reflect reflect = this.reflects.getOrDefault(className, null);

    if (reflect == null) {
      reflect = new Reflect();
      this.reflects.put(className, reflect);
      LOGGER.log(LOGLEVEL, "creating new Element");
    } else {
      LOGGER.log(LOGLEVEL, "appending to previous Element");
    }

    reflect.name(className);

    return reflect;
  }

  @Override
  public boolean process(
      final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

    if (roundEnv.processingOver()) {

      writeOutput();

    } else {

      processingReflectableImports(roundEnv);
      processingReflectable(roundEnv);
      processReflectableClasses(roundEnv);
    }

    return true;
  }

  /**
   * Process Class.
   *
   * @param reflect {@link Reflect}
   * @param reflectable {@link Reflectable}
   * @return {@link Reflect}
   */
  private Reflect processClass(final Reflect reflect, final Reflectable reflectable) {

    LOGGER.log(LOGLEVEL, "processClass " + reflect.name());
    reflect
        .allDeclaredConstructors(Boolean.valueOf(reflectable.allDeclaredConstructors()))
        .allDeclaredFields(Boolean.valueOf(reflectable.allDeclaredFields()))
        .allDeclaredMethods(Boolean.valueOf(reflectable.allDeclaredMethods()))
        .allPublicConstructors(Boolean.valueOf(reflectable.allPublicConstructors()))
        .allPublicFields(Boolean.valueOf(reflectable.allPublicFields()))
        .allPublicMethods(Boolean.valueOf(reflectable.allPublicMethods()));

    return reflect;
  }

  /**
   * Process Class.
   *
   * @param reflect {@link Reflect}
   * @param reflectable {@link ReflectableClass}
   * @return {@link Reflect}
   */
  private Reflect processClass(final Reflect reflect, final ReflectableClass reflectable) {

    LOGGER.log(LOGLEVEL, "processClass " + reflect.name());
    reflect
        .allDeclaredConstructors(Boolean.valueOf(reflectable.allDeclaredConstructors()))
        .allDeclaredFields(Boolean.valueOf(reflectable.allDeclaredFields()))
        .allDeclaredMethods(Boolean.valueOf(reflectable.allDeclaredMethods()))
        .allPublicConstructors(Boolean.valueOf(reflectable.allPublicConstructors()))
        .allPublicFields(Boolean.valueOf(reflectable.allPublicFields()))
        .allPublicMethods(Boolean.valueOf(reflectable.allPublicMethods()));

    return reflect;
  }

  /**
   * Process Imported Class using {@link Class}.
   *
   * @param clazz {@link Class}
   */
  private void processImportedClass(final String clazz) {
    try {
      Class<?> forName = Class.forName(clazz);
      Reflectable reflectable = forName.getAnnotation(Reflectable.class);
      Reflect reflect = getReflect(clazz);

      if (reflectable != null) {
        processClass(reflect, reflectable);
      }

      for (Field field : forName.getDeclaredFields()) {
        Reflectable reflection = field.getAnnotation(Reflectable.class);
        if (reflection != null) {
          LOGGER.log(LOGLEVEL, "adding Field " + field.getName() + " to " + clazz);
          reflect.addField(field.getName(), reflection.allowWrite(), false);
        }
      }

      for (Method method : forName.getMethods()) {
        Reflectable reflection = method.getAnnotation(Reflectable.class);
        if (reflection != null) {
          List<String> parameterTypes =
              Arrays.asList(method.getParameters()).stream()
                  .map(p -> p.getParameterizedType().getTypeName())
                  .collect(Collectors.toList());

          LOGGER.log(LOGLEVEL, "adding Method " + method.getName() + " to " + clazz);
          reflect.addMethod(method.getName(), parameterTypes);
        }
      }

    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Process Imported Classes.
   *
   * @param element {@link Element}
   */
  private void processImportedClasses(final Element element) {

    List<String> classNames = findClasses(element, "classes");
    for (String clazz : classNames) {
      LOGGER.log(LOGLEVEL, "processing ImportedClass " + clazz);
      processImportedClass(clazz);
    }
  }

  /**
   * Process Importing of Files.
   *
   * @param element {@link Element}
   */
  @SuppressWarnings("unchecked")
  private void processImportFiles(final Element element) {
    ReflectableImport[] reflectImports = element.getAnnotationsByType(ReflectableImport.class);

    for (ReflectableImport reflectImport : reflectImports) {

      for (String file : reflectImport.files()) {

        if (file.length() > 0) {
          try {

            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource(file);
            String data = Files.readString(new File(resource.getFile()).toPath());
            List<Map<String, Object>> list = this.gson.fromJson(data, List.class);

            for (Map<String, Object> map : list) {
              Reflect reflect = getReflect(map.get("name").toString());
              reflect.data(map);
            }

          } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  /**
   * Processing classes with 'Reflectable' annotation.
   *
   * @param roundEnv {@link RoundEnvironment}
   */
  private void processingReflectable(final RoundEnvironment roundEnv) {

    for (Element element : roundEnv.getElementsAnnotatedWith(Reflectable.class)) {

      String className = getClassName(element);
      LOGGER.log(LOGLEVEL, "processing 'Reflectable' annotation on class " + className);

      Reflectable[] reflectables = element.getAnnotationsByType(Reflectable.class);

      Reflect reflect = getReflect(className);
      for (Reflectable reflectable : reflectables) {

        switch (element.getKind()) {
          case FIELD:
            String fieldName = element.getSimpleName().toString();
            LOGGER.log(LOGLEVEL, "adding Field " + fieldName + " to " + className);
            reflect.addField(fieldName, reflectable.allowWrite(), false);
            break;
          case CONSTRUCTOR:
          case METHOD:
            String methodName = element.getSimpleName().toString();

            List<String> parameterTypes =
                ((ExecutableElement) element)
                    .getParameters().stream()
                        .map(param -> param.asType().toString())
                        .collect(Collectors.toList());

            LOGGER.log(LOGLEVEL, "adding Method " + methodName + " to " + className);
            reflect.addMethod(methodName, parameterTypes);

            break;
          case ENUM:
          case CLASS:
            reflect = processClass(reflect, reflectable);
            break;
          default:
            break;
        }
      }
    }
  }

  /**
   * Load Reflectable Imports.
   *
   * @param roundEnv {@link RoundEnvironment}s
   */
  private void processingReflectableImports(final RoundEnvironment roundEnv) {

    for (Element element : roundEnv.getElementsAnnotatedWith(ReflectableImport.class)) {
      processImportedClasses(element);
      processImportFiles(element);
    }
  }

  /**
   * Processing classes with 'ReflectableClass' annotation.
   *
   * @param reflectable {@link ReflectableClass}
   */
  private void processReflectableClass(final ReflectableClass reflectable) {

    String className = null;
    try {
      reflectable.className();
    } catch (MirroredTypeException e) {

      TypeMirror typeMirror = e.getTypeMirror();
      TypeElement asTypeElement = asTypeElement(typeMirror);

      className = asTypeElement.getQualifiedName().toString();

      try {
        className = forName(className).getName();
      } catch (ClassNotFoundException ee) {
        LOGGER.log(Level.WARNING, "cannot find class " + className);
      }
    }

    Reflect reflect = getReflect(className);
    reflect = processClass(reflect, reflectable);

    for (ReflectableField field : reflectable.fields()) {
      LOGGER.log(LOGLEVEL, "adding Field " + field.name() + " to " + className);
      reflect.addField(field.name(), field.allowWrite(), field.allowUnsafeAccess());
    }

    for (ReflectableMethod method : reflectable.methods()) {
      LOGGER.log(LOGLEVEL, "adding Method " + method.name() + " to " + className);
      reflect.addMethod(method.name(), Arrays.asList(method.parameterTypes()));
    }
  }

  /**
   * Processing classes with 'ReflectableClasses' and 'ReflectableClass' annotation.
   *
   * @param roundEnv {@link RoundEnvironment}
   */
  private void processReflectableClasses(final RoundEnvironment roundEnv) {

    for (Element element : roundEnv.getElementsAnnotatedWith(ReflectableClasses.class)) {

      String className = getClassName(element);
      LOGGER.log(LOGLEVEL, "processing 'ReflectableClasses' annotation on class " + className);

      ReflectableClasses[] reflectables = element.getAnnotationsByType(ReflectableClasses.class);

      for (ReflectableClasses reflectable : reflectables) {

        ReflectableClass[] classes = reflectable.value();
        for (ReflectableClass clazz : classes) {
          processReflectableClass(clazz);
        }
      }
    }

    Set<? extends Element> reflectableClasses =
        roundEnv.getElementsAnnotatedWithAny(
            Set.of(ReflectableClass.class, ReflectableClass.ReflectableClasses.class));

    for (Element element : reflectableClasses) {
      String className = getClassName(element);
      LOGGER.log(LOGLEVEL, "processing 'ReflectableClasses' annotation on class " + className);

      ReflectableClass[] reflectables = element.getAnnotationsByType(ReflectableClass.class);
      for (ReflectableClass clazz : reflectables) {
        processReflectableClass(clazz);
      }
    }
  }

  private String removePartsContainingDotFollowedByCapital(final String input) {
    return Arrays.stream(input.split("\\."))
        .filter(part -> !part.matches("\\p{Upper}.*"))
        .collect(Collectors.joining("."));
  }

  /** Write Output File. */
  private void writeOutput() {

    try {

      String name = generateReflectConfigPath(this.reflects.keySet());

      FileObject file =
          this.processingEnv
              .getFiler()
              .createResource(
                  StandardLocation.CLASS_OUTPUT,
                  "",
                  "META-INF/native-image/" + name + "/reflect-config.json");

      List<Map<String, Object>> data =
          this.reflects.values().stream().map(r -> r.data()).collect(Collectors.toList());

      try (Writer w = new OutputStreamWriter(file.openOutputStream(), "UTF-8")) {
        w.write(this.gson.toJson(data));
      }

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
