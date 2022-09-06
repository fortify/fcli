/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.appversion_auth_entity.cli.cmd;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.util.JsonHelper;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.util.SSCOutputHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = "update")
public class SSCAppVersionAuthEntityUpdateCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    private static final String AUTH_ENTITIES_ALL = "allAuthEntities";
    private static final String AUTH_ENTITIES_ASSIGNED = "assignedAuthEntities";

    @Mixin private SSCAppVersionResolverMixin.For parentVersionHandler;
    @Mixin private OutputMixin outputMixin;
    
    @Option(names={"-a", "--add"})
    private String[] authEntitiesToAdd;
    
    @Option(names={"-r", "--rm"})
    private String[] authEntitiesToRemove;
    
    @Override
    protected Void runWithUnirest(UnirestInstance unirest) {
        String applicationVersionId = parentVersionHandler.getApplicationVersionId(unirest);
        outputMixin.write(
            unirest.put(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(applicationVersionId))
                .body(generateAuthEntityUpdateBody(unirest, applicationVersionId))
        );
        
        return null;
    }

    private ArrayNode generateAuthEntityUpdateBody(UnirestInstance unirest, String applicationVersionId) {
        boolean hasAdd = authEntitiesToAdd!=null && authEntitiesToAdd.length>0; 
        SSCBulkResponse authEntities = getAuthEntitiesBulkResponse(unirest, applicationVersionId, hasAdd);
        ArrayNode updateArray = new ObjectMapper().createArrayNode();
        updateArray.addAll(JsonHelper.stream((ArrayNode)authEntities.data(AUTH_ENTITIES_ASSIGNED))
                .filter(new SSCAuthEntityMatcher(authEntitiesToRemove, MatchMode.EXCLUDE))
                .collect(JsonHelper.arrayNodeCollector()));
        if ( hasAdd ) { 
            updateArray.addAll(JsonHelper.stream((ArrayNode)authEntities.data(AUTH_ENTITIES_ALL))
                .filter(new SSCAuthEntityMatcher(authEntitiesToAdd, MatchMode.INCLUDE))
                .collect(JsonHelper.arrayNodeCollector()));
        }
        return updateArray;
    }

    private SSCBulkResponse getAuthEntitiesBulkResponse(UnirestInstance unirest, String applicationVersionId, boolean includeAll) {
        SSCBulkRequestBuilder bulkRequestBuilder = new SSCBulkRequestBuilder()
            .request(AUTH_ENTITIES_ASSIGNED, getExistingAuthEntitiesRequest(unirest, applicationVersionId));
        if (includeAll) {
            bulkRequestBuilder.request(AUTH_ENTITIES_ALL, getAllAuthEntitiesRequest(unirest));
        }
        return bulkRequestBuilder.execute(unirest);
    }

    private GetRequest getAllAuthEntitiesRequest(UnirestInstance unirest) {
        return unirest.get(SSCUrls.AUTH_ENTITIES).queryString("limit","-1");
    }

    private GetRequest getExistingAuthEntitiesRequest(UnirestInstance unirest, String applicationVersionId) {
        return unirest.get(SSCUrls.PROJECT_VERSION_AUTH_ENTITIES(applicationVersionId))
                .queryString("limit","-1");
    }
    
    private static enum MatchMode { INCLUDE, EXCLUDE }; 
    @RequiredArgsConstructor
    private static final class SSCAuthEntityMatcher implements Predicate<JsonNode> {
        private final String[] authEntities;
        private final MatchMode matchMode;
        private final Set<String> previousMatchedAuthEntities = new HashSet<>();
        
        @Override
        public boolean test(JsonNode node) {
            Set<String> values = new HashSet<>(Arrays.asList( new String[]{
                    getLowerCase(node, "id"),
                    getLowerCase(node, "entityName"),
                    getLowerCase(node, "email")
            } )); 
            boolean isMatching = 
                    authEntities!=null &&
                    Stream.of(authEntities).map(String::toLowerCase)
                    .filter(values::contains)
                    .filter(this::hasPreviousMatch)
                    .count() > 0;
            return matchMode==MatchMode.INCLUDE ? isMatching : !isMatching;
        }

        private String getLowerCase(JsonNode node, String field) {
            String result = JsonHelper.evaluateJsonPath(node, field, String.class);
            return result == null ? null : result.toLowerCase();
        }
        
        private boolean hasPreviousMatch(String authEntity) {
            if ( !previousMatchedAuthEntities.add(authEntity) ) {
                throw new IllegalArgumentException(String.format("Multiple records match '%s'; please use a unique identifier", authEntity));
            }
            return true;
        }
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputHelper.defaultTableOutputConfig()
            .defaultColumns("id#entityName:Name#displayName#type#email#isLdap");
    }
}
