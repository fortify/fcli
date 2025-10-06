package com.fortify.cli.ssc.custom_tag.helper;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public class SSCCustomTagDescriptor extends JsonNodeHolder {
    private String customTagType;
    private boolean deletable;
    private String guid;
    private String id;
    private String name;
    private boolean primaryTag;
    
    public boolean isEqualById(SSCCustomTagDescriptor other) {
        return other!=null && this.id!=null && this.id.equals(other.id);
    }
}