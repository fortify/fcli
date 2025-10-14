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
import java.util.ArrayList;
import java.util.List;

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
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}SourceLocation" minOccurs="0"/&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}Context" minOccurs="0"/&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}StructuralMatch" maxOccurs="unbounded" minOccurs="0"/&gt;
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
    "sourceLocation",
    "context",
    "structuralMatch"
})
@XmlRootElement(name = "Structural")
public class Structural {

    @XmlElement(name = "SourceLocation")
    protected SourceLocationType sourceLocation;
    @XmlElement(name = "Context")
    protected Context context;
    @XmlElement(name = "StructuralMatch")
    protected List<StructuralMatch> structuralMatch;

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

    /**
     * Gets the value of the context property.
     * 
     * @return
     *     possible object is
     *     {@link Context }
     *     
     */
    public Context getContext() {
        return context;
    }

    /**
     * Sets the value of the context property.
     * 
     * @param value
     *     allowed object is
     *     {@link Context }
     *     
     */
    public void setContext(Context value) {
        this.context = value;
    }

    /**
     * Gets the value of the structuralMatch property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the structuralMatch property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStructuralMatch().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link StructuralMatch }
     * 
     * 
     */
    public List<StructuralMatch> getStructuralMatch() {
        if (structuralMatch == null) {
            structuralMatch = new ArrayList<StructuralMatch>();
        }
        return this.structuralMatch;
    }

}
