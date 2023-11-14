/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.system_state.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper=true)
public class SSCJobDescriptor extends JsonNodeHolder {
    private String jobName;
    private String jobGroup;
    private Integer priority;
    private String jobState;

    public SSCJobDescriptor[] getJobDescriptors(UnirestInstance unirest){
        String col = jobName;
        return Stream.of(col).map(id-> SSCJobHelper.getJobDescriptor(unirest, id)).toArray(SSCJobDescriptor[]::new);
    }
    public Collection<JsonNode> getJobDescriptorJsonNodes(UnirestInstance unirest){
        return Stream.of(getJobDescriptors(unirest)).map(SSCJobDescriptor::asJsonNode).collect(Collectors.toList());
    }
}
