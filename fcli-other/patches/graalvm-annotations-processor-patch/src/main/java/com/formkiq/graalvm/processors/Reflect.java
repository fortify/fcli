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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holder class for Graalvm Reflect config file. */
public class Reflect {

  /** Data holder. */
  private Map<String, Object> data = new HashMap<>();

  /** constructor. */
  public Reflect() {}

  /**
   * Add Field.
   *
   * @param fieldName {@link String}
   * @param allowWrite boolean
   * @param allowUnsafeAccess boolean
   */
  @SuppressWarnings("unchecked")
  public void addField(
      final String fieldName, final boolean allowWrite, final boolean allowUnsafeAccess) {

    if (!this.data.containsKey("methods")) {
      this.data.put("methods", new ArrayList<>());
    }

    if (!this.data.containsKey("fields")) {
      this.data.put("fields", new ArrayList<>());
    }

    Map<String, Object> map =
        new HashMap<>(Map.of("allowWrite", Boolean.valueOf(allowWrite), "name", fieldName));
    if (allowUnsafeAccess) {
      map.put("allowUnsafeAccess", Boolean.valueOf(allowUnsafeAccess));
    }

    ((List<Map<String, Object>>) this.data.get("fields")).add(map);
  }

  /**
   * Add Method.
   *
   * @param methodName {@link String}
   * @param parameterTypes {@link List} {@link String}
   */
  @SuppressWarnings("unchecked")
  public void addMethod(final String methodName, final List<String> parameterTypes) {

    if (!this.data.containsKey("methods")) {
      this.data.put("methods", new ArrayList<>());
    }

    if (!this.data.containsKey("fields")) {
      this.data.put("fields", new ArrayList<>());
    }

    Map<String, Object> map = Map.of("parameterTypes", parameterTypes, "name", methodName);
    ((List<Map<String, Object>>) this.data.get("methods")).add(map);
  }

  /**
   * Set allDeclaredConstructors.
   *
   * @param allDeclaredConstructors {@link Boolean}
   * @return {@link Reflect}
   */
  public Reflect allDeclaredConstructors(final Boolean allDeclaredConstructors) {
    this.data.put("allDeclaredConstructors", allDeclaredConstructors);
    return this;
  }

  /**
   * Get AllDeclaredFields.
   *
   * @return {@link Boolean}
   */
  public Boolean allDeclaredFields() {
    return (Boolean) this.data.get("allDeclaredFields");
  }

  /**
   * Get AllDeclaredFields.
   *
   * @param allDeclaredFields {@link Boolean}
   * @return {@link Reflect}
   */
  public Reflect allDeclaredFields(final Boolean allDeclaredFields) {
    this.data.put("allDeclaredFields", allDeclaredFields);
    return this;
  }

  /**
   * Get allDeclaredMethods.
   *
   * @return {@link Boolean}
   */
  public Boolean allDeclaredMethods() {
    return (Boolean) this.data.get("allDeclaredMethods");
  }

  /**
   * Set AllDeclaredMethods.
   *
   * @param allDeclaredMethods {@link Boolean}
   * @return {@link Reflect}
   */
  public Reflect allDeclaredMethods(final Boolean allDeclaredMethods) {
    this.data.put("allDeclaredMethods", allDeclaredMethods);
    return this;
  }

  /**
   * Get AllPublicConstructors.
   *
   * @return {@link Boolean}
   */
  public Boolean allPublicConstructors() {
    return (Boolean) this.data.get("allPublicConstructors");
  }

  /**
   * Set AllPublicConstructors.
   *
   * @param allPublicConstructors {@link Boolean}
   * @return {@link Reflect}
   */
  public Reflect allPublicConstructors(final Boolean allPublicConstructors) {
    this.data.put("allPublicConstructors", allPublicConstructors);
    return this;
  }

  /**
   * Get allPublicFields.
   *
   * @return {@link Boolean}
   */
  public Boolean allPublicFields() {
    return (Boolean) this.data.get("allPublicFields");
  }

  /**
   * Set allPublicFields.
   *
   * @param allPublicFields {@link Boolean}
   * @return {@link Reflect}
   */
  public Reflect allPublicFields(final Boolean allPublicFields) {
    this.data.put("allPublicFields", allPublicFields);
    return this;
  }

  /**
   * Get AllPublicMethods.
   *
   * @return {@link Boolean}
   */
  public Boolean allPublicMethods() {
    return (Boolean) this.data.get("allPublicMethods");
  }

  /**
   * Set AllPublicMethods.
   *
   * @param allPublicMethods {@link Boolean}
   * @return {@link Reflect}
   */
  public Reflect allPublicMethods(final Boolean allPublicMethods) {
    this.data.put("allPublicMethods", allPublicMethods);
    return this;
  }

  /**
   * Get {@link Map} Data.
   *
   * @return {@link Map}
   */
  public Map<String, Object> data() {
    return this.data;
  }

  /**
   * Set {@link Map} Data.
   *
   * @param map {@link Map}
   * @return {@link Reflect}
   */
  public Reflect data(final Map<String, Object> map) {
    this.data = map;
    return this;
  }

  /**
   * Get {@link List} {@link Map}.
   *
   * @return {@link List} {@link Map}
   */
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> fields() {
    return (List<Map<String, Object>>) this.data.get("fields");
  }

  /**
   * Get {@link Map}.
   *
   * @param list {@link List} {@link Map}
   * @return {@link Reflect}
   */
  public Reflect fields(final List<Map<String, Object>> list) {
    this.data.put("fields", list);
    return this;
  }

  /**
   * Get AllDeclaredConstructors.
   *
   * @return {@link Boolean}
   */
  public Boolean getAllDeclaredConstructors() {
    return (Boolean) this.data.get("allDeclaredConstructors");
  }

  /**
   * Get Name.
   *
   * @return {@link String}
   */
  public String name() {
    return (String) this.data.get("name");
  }

  /**
   * Set Name.
   *
   * @param name {@link String}
   * @return {@link Reflect}
   */
  public Reflect name(final String name) {
    this.data.put("name", name);
    return this;
  }
}
