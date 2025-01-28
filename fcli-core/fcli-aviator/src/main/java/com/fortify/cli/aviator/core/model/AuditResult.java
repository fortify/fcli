package com.fortify.cli.aviator.core.model;


import com.formkiq.graalvm.annotations.Reflectable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "audit")
@AllArgsConstructor
@Builder
@Reflectable
public class AuditResult{
    @XmlElement(name = "value")
    public String tagValue;
    @XmlElement(name = "comment")
    public String comment;

}
