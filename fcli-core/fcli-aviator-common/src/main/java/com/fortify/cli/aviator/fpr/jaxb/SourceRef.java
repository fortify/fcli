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
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}FunctionCall"/&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}FunctionEntry"/&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}Statement"/&gt;
 *       &lt;/choice&gt;
 *       &lt;attribute name="sourceRefID" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "functionCall",
    "functionEntry",
    "statement"
})
@XmlRootElement(name = "SourceRef")
public class SourceRef {

    @XmlElement(name = "FunctionCall")
    protected FunctionCall functionCall;
    @XmlElement(name = "FunctionEntry")
    protected FunctionEntry functionEntry;
    @XmlElement(name = "Statement")
    protected Statement statement;
    @XmlAttribute(name = "sourceRefID")
    protected String sourceRefID;

    /**
     * Gets the value of the functionCall property.
     * 
     * @return
     *     possible object is
     *     {@link FunctionCall }
     *     
     */
    public FunctionCall getFunctionCall() {
        return functionCall;
    }

    /**
     * Sets the value of the functionCall property.
     * 
     * @param value
     *     allowed object is
     *     {@link FunctionCall }
     *     
     */
    public void setFunctionCall(FunctionCall value) {
        this.functionCall = value;
    }

    /**
     * Gets the value of the functionEntry property.
     * 
     * @return
     *     possible object is
     *     {@link FunctionEntry }
     *     
     */
    public FunctionEntry getFunctionEntry() {
        return functionEntry;
    }

    /**
     * Sets the value of the functionEntry property.
     * 
     * @param value
     *     allowed object is
     *     {@link FunctionEntry }
     *     
     */
    public void setFunctionEntry(FunctionEntry value) {
        this.functionEntry = value;
    }

    /**
     * Gets the value of the statement property.
     * 
     * @return
     *     possible object is
     *     {@link Statement }
     *     
     */
    public Statement getStatement() {
        return statement;
    }

    /**
     * Sets the value of the statement property.
     * 
     * @param value
     *     allowed object is
     *     {@link Statement }
     *     
     */
    public void setStatement(Statement value) {
        this.statement = value;
    }

    /**
     * Gets the value of the sourceRefID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSourceRefID() {
        return sourceRefID;
    }

    /**
     * Sets the value of the sourceRefID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSourceRefID(String value) {
        this.sourceRefID = value;
    }

}
