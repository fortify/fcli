package com.fortify.cli.ssc.entity.job.helper;

import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess
@Data @EqualsAndHashCode(callSuper=true)
public class SSCJobDescriptor extends JsonNodeHolder {
    private String jobName;
    private String jobGroup;
    private Integer priority;
    private String jobState;
}
