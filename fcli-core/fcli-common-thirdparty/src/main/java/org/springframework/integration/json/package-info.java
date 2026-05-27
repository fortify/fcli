/**
 * This package contains a copy of two classes from spring-integration that
 * fcli depends on, to avoid having to pull in the full spring-integration 
 * dependency and transitive dependencies. Compared to the original version,
 * there's one minor change; the JsonNodeWrapper interface has been changed
 * to public to allow access to the original JsonNode instance if an SpEL
 * expression returns a JsonNode, as used by JsonHelper::evaluateSpelExpression
 * (through JsonHelper::unwrapSpelExpressionResult).
 */
package org.springframework.integration.json;

