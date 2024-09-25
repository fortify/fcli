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
package com.fortify.cli.sc_sast.sensor_pool.cli.mixin;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.sc_sast.sensor_pool.cli.helper.SCSastSensorPoolDescriptor;
import com.fortify.cli.sc_sast.sensor_pool.cli.helper.SCSastSensorPoolHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SCSastSensorPoolResolverMixin {
    public static abstract class AbstractSCSastSensorPoolResolverMixin {
        public abstract String getSensorPoolNameOrUuid();

        public SCSastSensorPoolDescriptor getSensorPoolDescriptor(UnirestInstance unirest){
            return SCSastSensorPoolHelper.getRequiredSensorPool(unirest, getSensorPoolNameOrUuid());
        }
        
        public String getSensorPoolUuid(UnirestInstance unirest) {
            return getSensorPoolDescriptor(unirest).getUuid();
        }
        
        public final boolean hasValue() { return StringUtils.isNotBlank(getSensorPoolNameOrUuid()); }
    }
    
    public static final class OptionalOption extends AbstractSCSastSensorPoolResolverMixin {
        @Option(names = {"--sensor-pool", "--pool"}, required = false, descriptionKey = "fcli.sc-sast.sensor-pool.resolver.nameOrUuid")
        @Getter private String sensorPoolNameOrUuid;
    }
    
    public static final class RequiredOption extends AbstractSCSastSensorPoolResolverMixin {
        @Option(names = {"--sensor-pool", "--pool"}, required = true, descriptionKey = "fcli.sc-sast.sensor-pool.resolver.nameOrUuid")
        @Getter private String sensorPoolNameOrUuid;
    }
    
    public static final class PositionalParameter extends AbstractSCSastSensorPoolResolverMixin {
        @EnvSuffix("SENSORPOOL") @Parameters(index = "0", arity = "1", descriptionKey = "fcli.sc-sast.sensor-pool.resolver.nameOrUuid")
        @Getter private String sensorPoolNameOrUuid;
    }
}
