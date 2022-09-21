package com.fortify.cli.ssc.appversion_filterset.helper;

import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper=true)
public class SSCAppVersionFilterSetDescriptor extends JsonNodeHolder {
    private String guid;
    private String title;
    private boolean defaultFilterSet;
}
