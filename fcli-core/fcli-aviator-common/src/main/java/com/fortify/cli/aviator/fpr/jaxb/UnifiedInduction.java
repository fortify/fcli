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
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * An induction provides supporting evidence for a node.
 * 
 * <p>Java class for UnifiedInduction complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UnifiedInduction"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Text" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="TraceRef" type="{xmlns://www.fortifysoftware.com/schema/fvdl}UnifiedTraceRef"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}int" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UnifiedInduction", propOrder = {
    "text",
    "traceRef"
})
public class UnifiedInduction {

    @XmlElement(name = "Text", required = true)
    protected String text;
    @XmlElement(name = "TraceRef", required = true)
    protected UnifiedTraceRef traceRef;
    @XmlAttribute(name = "id", required = true)
    protected int id;

    /**
     * Gets the value of the text property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the value of the text property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setText(String value) {
        this.text = value;
    }

    /**
     * Gets the value of the traceRef property.
     * 
     * @return
     *     possible object is
     *     {@link UnifiedTraceRef }
     *     
     */
    public UnifiedTraceRef getTraceRef() {
        return traceRef;
    }

    /**
     * Sets the value of the traceRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link UnifiedTraceRef }
     *     
     */
    public void setTraceRef(UnifiedTraceRef value) {
        this.traceRef = value;
    }

    /**
     * Gets the value of the id property.
     * 
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     */
    public void setId(int value) {
        this.id = value;
    }

}
