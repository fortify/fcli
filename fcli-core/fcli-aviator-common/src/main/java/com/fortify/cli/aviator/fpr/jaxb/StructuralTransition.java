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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}Function"/&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}SourceLocation" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "function",
    "sourceLocation"
})
@XmlRootElement(name = "StructuralTransition")
public class StructuralTransition {

    @XmlElement(name = "Function", required = true)
    protected Function function;
    @XmlElement(name = "SourceLocation")
    protected SourceLocationType sourceLocation;

    /**
     * Gets the value of the function property.
     * 
     * @return
     *     possible object is
     *     {@link Function }
     *     
     */
    public Function getFunction() {
        return function;
    }

    /**
     * Sets the value of the function property.
     * 
     * @param value
     *     allowed object is
     *     {@link Function }
     *     
     */
    public void setFunction(Function value) {
        this.function = value;
    }

    /**
     * Gets the value of the sourceLocation property.
     * 
     * @return
     *     possible object is
     *     {@link SourceLocationType }
     *     
     */
    public SourceLocationType getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Sets the value of the sourceLocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link SourceLocationType }
     *     
     */
    public void setSourceLocation(SourceLocationType value) {
        this.sourceLocation = value;
    }

}
