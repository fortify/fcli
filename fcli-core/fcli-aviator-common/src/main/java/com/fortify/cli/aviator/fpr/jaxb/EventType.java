/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator.fpr.jaxb;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for eventType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;simpleType name="eventType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Source"/&gt;
 *     &lt;enumeration value="Sink"/&gt;
 *     &lt;enumeration value="Trace"/&gt;
 *     &lt;enumeration value="Validation"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "eventType")
@XmlEnum
public enum EventType {

    @XmlEnumValue("Source")
    SOURCE("Source"),
    @XmlEnumValue("Sink")
    SINK("Sink"),
    @XmlEnumValue("Trace")
    TRACE("Trace"),
    @XmlEnumValue("Validation")
    VALIDATION("Validation");
    private final String value;

    EventType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EventType fromValue(String v) {
        for (EventType c: EventType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
