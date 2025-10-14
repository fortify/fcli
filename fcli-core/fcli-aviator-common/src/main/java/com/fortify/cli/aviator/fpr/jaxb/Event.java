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
 *       &lt;sequence&gt;
 *         &lt;element name="PrimaryNode" type="{xmlns://www.fortifysoftware.com/schema/fvdl}UnifiedNode"/&gt;
 *         &lt;element ref="{xmlns://www.fortifysoftware.com/schema/fvdl}ProgramState"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="type" use="required" type="{xmlns://www.fortifysoftware.com/schema/fvdl}eventType" /&gt;
 *       &lt;attribute name="configurationId" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="eventId" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "primaryNode",
    "programState"
})
@XmlRootElement(name = "Event")
public class Event {

    @XmlElement(name = "PrimaryNode", required = true)
    protected UnifiedNode primaryNode;
    @XmlElement(name = "ProgramState", required = true)
    protected ProgramState programState;
    @XmlAttribute(name = "type", required = true)
    protected EventType type;
    @XmlAttribute(name = "configurationId")
    protected String configurationId;
    @XmlAttribute(name = "eventId")
    protected String eventId;

    /**
     * Gets the value of the primaryNode property.
     * 
     * @return
     *     possible object is
     *     {@link UnifiedNode }
     *     
     */
    public UnifiedNode getPrimaryNode() {
        return primaryNode;
    }

    /**
     * Sets the value of the primaryNode property.
     * 
     * @param value
     *     allowed object is
     *     {@link UnifiedNode }
     *     
     */
    public void setPrimaryNode(UnifiedNode value) {
        this.primaryNode = value;
    }

    /**
     * Gets the value of the programState property.
     * 
     * @return
     *     possible object is
     *     {@link ProgramState }
     *     
     */
    public ProgramState getProgramState() {
        return programState;
    }

    /**
     * Sets the value of the programState property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProgramState }
     *     
     */
    public void setProgramState(ProgramState value) {
        this.programState = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link EventType }
     *     
     */
    public EventType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link EventType }
     *     
     */
    public void setType(EventType value) {
        this.type = value;
    }

    /**
     * Gets the value of the configurationId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConfigurationId() {
        return configurationId;
    }

    /**
     * Sets the value of the configurationId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConfigurationId(String value) {
        this.configurationId = value;
    }

    /**
     * Gets the value of the eventId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the value of the eventId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEventId(String value) {
        this.eventId = value;
    }

}
