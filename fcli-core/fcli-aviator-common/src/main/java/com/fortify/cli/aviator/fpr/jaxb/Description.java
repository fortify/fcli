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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{xmlns://www.fortifysoftware.com/schema/fvdl}descriptionBaseType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}CustomDescription" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="classID" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "customDescription"
})
public class Description
    extends DescriptionBaseType
{

    @XmlElement(name = "CustomDescription")
    protected List<CustomDescription> customDescription;
    @XmlAttribute(name = "classID")
    protected String classID;

    /**
     * Gets the value of the customDescription property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the customDescription property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCustomDescription().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CustomDescription }
     * 
     * 
     */
    public List<CustomDescription> getCustomDescription() {
        if (customDescription == null) {
            customDescription = new ArrayList<CustomDescription>();
        }
        return this.customDescription;
    }

    /**
     * Gets the value of the classID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClassID() {
        return classID;
    }

    /**
     * Sets the value of the classID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClassID(String value) {
        this.classID = value;
    }

}
