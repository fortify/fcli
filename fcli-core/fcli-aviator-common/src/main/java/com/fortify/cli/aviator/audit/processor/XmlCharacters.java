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
package com.fortify.cli.aviator.audit.processor;

import java.util.LinkedHashSet;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Characters that XML 1.0 does not allow, not even as a character reference such as {@code &#24;}. They can turn up in
 * source code or in text from the LLM. Depending on the JDK, the XML serializer then either fails or writes a character
 * reference that no XML 1.0 parser accepts, which makes the generated file unusable.
 */
final class XmlCharacters {
    private XmlCharacters() {
    }

    /** The Char production of XML 1.0: tab, line feed, carriage return, and everything from U+0020 except U+FFFE and U+FFFF. */
    static boolean isValid(int codePoint) {
        return codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }

    static boolean hasInvalid(String text) {
        return text != null && text.codePoints().anyMatch(codePoint -> !isValid(codePoint));
    }

    /** Distinct invalid characters in the text as code points, like {@code U+0018}, for log messages. */
    static String describeInvalid(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text != null) {
            text.codePoints().filter(codePoint -> !isValid(codePoint))
                    .forEach(codePoint -> found.add(String.format("U+%04X", codePoint)));
        }
        return String.join(", ", found);
    }

    static String strip(String text) {
        if (!hasInvalid(text)) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        text.codePoints().filter(XmlCharacters::isValid).forEach(result::appendCodePoint);
        return result.toString();
    }

    /**
     * Removes the invalid characters from the text, CDATA and attribute values below the node.
     *
     * @return the number of characters removed
     */
    static int stripInvalid(Node node) {
        int removed = 0;
        if (node instanceof CharacterData data) {
            String value = data.getData();
            if (hasInvalid(value)) {
                String stripped = strip(value);
                removed += value.length() - stripped.length();
                data.setData(stripped);
            }
        }
        if (node instanceof Attr attribute) {
            String value = attribute.getValue();
            if (hasInvalid(value)) {
                String stripped = strip(value);
                removed += value.length() - stripped.length();
                attribute.setValue(stripped);
            }
        }
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; attributes != null && i < attributes.getLength(); i++) {
            removed += stripInvalid(attributes.item(i));
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            removed += stripInvalid(child);
        }
        return removed;
    }
}
