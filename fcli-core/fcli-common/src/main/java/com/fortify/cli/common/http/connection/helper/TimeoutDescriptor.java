package com.fortify.cli.common.http.connection.helper;

import java.util.Set;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor.ProxyMatchMode;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = false) 
@Builder
@Reflectable @NoArgsConstructor @AllArgsConstructor 
public class TimeoutDescriptor extends JsonNodeHolder{
    private String name;
    private Set<String> modules;
    private int timeout;
    private TimeoutType type;

    public enum TimeoutType {
        SOCKET, CONNECT
    }
    
    public boolean matchesModule(String module) {
        return modules==null || modules.contains(module);
    }
}
