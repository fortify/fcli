/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.aviator.fpr.utils;

import java.math.BigDecimal;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.jaxb.EngineData;
import com.fortify.cli.aviator.fpr.jaxb.MetaInfo;

/**
 * Utility class for XML-related operations, such as safe parsing of numbers
 * with defaults, and extracting meta information from rule elements.
 */
public class XmlUtils {
    private static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);

    /**
     * Safely parses a string to an integer, returning a default value on failure.
     *
     * @param value        String to parse
     * @param defaultValue Default if parsing fails
     * @return Parsed integer or default
     */
    public static int safeParseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse int: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Safely parses a string to a float, returning a default value on failure.
     *
     * @param value        String to parse
     * @param defaultValue Default if parsing fails
     * @return Parsed float or default
     */
    public static float safeParseFloat(String value, float defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse float: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Safely parses a string to a double, returning a default value on failure.
     *
     * @param value        String to parse
     * @param defaultValue Default if parsing fails
     * @return Parsed double or default
     */
    public static double safeParseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse double: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Safely parses a string to a BigDecimal, returning a default value on failure.
     *
     * @param value        String to parse
     * @param defaultValue Default if parsing fails
     * @return Parsed BigDecimal or default
     */
    public static BigDecimal safeParseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse BigDecimal: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Extracts meta information (accuracy, impact, probability, audience) from a rule element.
     *
     * @param ruleElement JAXB Rule element
     * @return Array of [accuracy, impact, probability, audience] as strings, defaults to "0"/""
     */
    public static String[] getMetaInfoFromRule(EngineData.RuleInfo.Rule ruleElement) {
        String[] metaInfo = new String[]{"0", "0", "0", ""};
        if (ruleElement == null) {
            return metaInfo;
        }

        MetaInfo metaInfoElem = ruleElement.getMetaInfo();
        if (metaInfoElem != null) {
            for (MetaInfo.Group group : metaInfoElem.getGroup()) {
                String groupName = group.getName().toLowerCase();
                String content = group.getValue().trim();

                switch (groupName) {
                    case "accuracy":
                        metaInfo[0] = content;
                        break;
                    case "impact":
                        metaInfo[1] = content;
                        break;
                    case "probability":
                        metaInfo[2] = content;
                        break;
                    case "audience":
                        metaInfo[3] = content;
                        break;
                }
            }
        }
        return metaInfo;
    }

    /**
     * Creates a {@link DocumentBuilderFactory} pre-configured to prevent XXE attacks.
     * Disables external general/parameter entities, external DTD loading, and XInclude,
     * and enables {@code FEATURE_SECURE_PROCESSING}.
     *
     * @param namespaceAware whether the factory should be namespace-aware
     * @return a hardened {@link DocumentBuilderFactory}
     * @throws IllegalStateException if the JDK does not support the required security features
     */
    public static DocumentBuilderFactory secureDocumentBuilderFactory(boolean namespaceAware) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setNamespaceAware(namespaceAware);
            return dbf;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Failed to configure secure XML DocumentBuilderFactory", e);
        }
    }

    /**
     * Creates a {@link DocumentBuilder} pre-configured to prevent XXE attacks.
     * Convenience method combining {@link #secureDocumentBuilderFactory(boolean)}
     * and {@link DocumentBuilderFactory#newDocumentBuilder()}.
     *
     * @param namespaceAware whether the builder should be namespace-aware
     * @return a hardened {@link DocumentBuilder}
     * @throws IllegalStateException if the JDK does not support the required security features
     */
    public static DocumentBuilder secureDocumentBuilder(boolean namespaceAware) {
        try {
            return secureDocumentBuilderFactory(namespaceAware).newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Failed to create secure XML DocumentBuilder", e);
        }
    }

    /**
     * Creates a {@link TransformerFactory} pre-configured to prevent XXE attacks.
     * Restricts access to external DTDs and stylesheets and enables {@code FEATURE_SECURE_PROCESSING}.
     *
     * @return a hardened {@link TransformerFactory}
     * @throws IllegalStateException if the JDK does not support the required security features
     */
    public static TransformerFactory secureTransformerFactory() {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            return tf;
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException("Failed to configure secure XML TransformerFactory", e);
        }
    }
}