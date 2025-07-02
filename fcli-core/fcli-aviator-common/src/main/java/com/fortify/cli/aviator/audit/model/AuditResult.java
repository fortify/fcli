package com.fortify.cli.aviator.audit.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

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