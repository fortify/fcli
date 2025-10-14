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
 *     &lt;extension base="{xmlns://www.fortifysoftware.com/schema/fvdl}sourceRefBaseType"&gt;
 *       &lt;all&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}Function"/&gt;
 *       &lt;/all&gt;
 *       &lt;attribute name="callType" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "function"
})
@XmlRootElement(name = "FunctionCall")
public class FunctionCall
    extends SourceRefBaseType
{

    @XmlElement(name = "Function", required = true)
    protected Function function;
    @XmlAttribute(name = "callType")
    protected String callType;

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
     * Gets the value of the callType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCallType() {
        return callType;
    }

    /**
     * Sets the value of the callType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCallType(String value) {
        this.callType = value;
    }

}
