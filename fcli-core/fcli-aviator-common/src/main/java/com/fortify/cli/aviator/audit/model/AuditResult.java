package com.fortify.cli.aviator.audit.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

import com.formkiq.graalvm.annotations.Reflectable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "audit")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Reflectable
public class AuditResult {
    @XmlElement(name = "value")
    public String tagValue;
    @XmlElement(name = "comment")
    public String comment;
    @XmlElement(name = "autoremediation")
    public Autoremediation autoremediation;
}