/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.sc_sast.scan.cli.mixin;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.variable.AbstractPredefinedVariableResolverMixin;
import com.fortify.cli.sc_sast.scan.cli.cmd.SCSastScanCommands;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobDescriptor;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

public class SCSastScanJobResolverMixin {
    @ReflectiveAccess
    public static abstract class AbstractSCSastScanJobResolverMixin extends AbstractPredefinedVariableResolverMixin {
        @Getter private Class<?> predefinedVariableClass = SCSastScanCommands.class;
        protected abstract String getNonResolvedScanJobToken();
        
        public SCSastControllerScanJobDescriptor getScanJobDescriptor(UnirestInstance unirest, Integer minStatusEndpointVersion) {
            return SCSastControllerScanJobHelper.getScanJobDescriptor(unirest, resolvePredefinedVariable(getNonResolvedScanJobToken()), minStatusEndpointVersion);
        }

        public SCSastControllerScanJobDescriptor getScanJobDescriptor(UnirestInstance unirest) {
            return getScanJobDescriptor(unirest, null);
        }
    }
    
    @ReflectiveAccess
    public static abstract class AbstractSCSastMultiScanJobResolverMixin extends AbstractPredefinedVariableResolverMixin {
        @Getter private Class<?> predefinedVariableClass = SCSastScanCommands.class;
        protected abstract String[] getNonResolvedScanJobTokens();
        
        public SCSastControllerScanJobDescriptor[] getScanJobDescriptors(UnirestInstance unirest, Integer minStatusEndpointVersion) {
            return Stream.of(getNonResolvedScanJobTokens()).map(id->SCSastControllerScanJobHelper.getScanJobDescriptor(unirest, resolvePredefinedVariable(id), minStatusEndpointVersion)).toArray(SCSastControllerScanJobDescriptor[]::new);
        }

        public SCSastControllerScanJobDescriptor[] getScanJobDescriptors(UnirestInstance unirest) {
            return getScanJobDescriptors(unirest, null);
        }
        
        public Collection<JsonNode> getScanJobDescriptorJsonNodes(UnirestInstance unirest, Integer minStatusEndpointVersion){
            return Stream.of(getScanJobDescriptors(unirest, minStatusEndpointVersion)).map(SCSastControllerScanJobDescriptor::asJsonNode).collect(Collectors.toList());
        }
        
        public Collection<JsonNode> getScanJobDescriptorJsonNodes(UnirestInstance unirest){
            return getScanJobDescriptorJsonNodes(unirest, null);
        }
    }
    
    @ReflectiveAccess
    public static class RequiredOption extends AbstractSCSastScanJobResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Option(names = {"--job", "--job-token"}, required = true)
        @Getter private String nonResolvedScanJobToken;
    }
    
    @ReflectiveAccess
    public static class PositionalParameter extends AbstractSCSastScanJobResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Parameters(index = "0", arity = "1", paramLabel="scan-job-token")
        @Getter private String nonResolvedScanJobToken;
    }
    
    @ReflectiveAccess
    public static class PositionalParameterMulti extends AbstractSCSastMultiScanJobResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Parameters(index = "0", arity = "1..", paramLabel = "scan-job-tokens")
        @Getter private String[] nonResolvedScanJobTokens;
    }
}
