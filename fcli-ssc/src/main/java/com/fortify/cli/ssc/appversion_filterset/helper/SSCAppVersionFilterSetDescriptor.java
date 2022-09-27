package com.fortify.cli.ssc.appversion_filterset.helper;

import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess
@Data @EqualsAndHashCode(callSuper=true)
public class SSCAppVersionFilterSetDescriptor extends JsonNodeHolder {
    private String guid;
    private String title;
    private boolean defaultFilterSet;
}
