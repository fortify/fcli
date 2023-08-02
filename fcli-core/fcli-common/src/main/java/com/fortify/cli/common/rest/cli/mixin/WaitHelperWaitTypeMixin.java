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
package com.fortify.cli.common.rest.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.rest.cli.mixin.WaitHelperWaitTypeMixin.AnyOrAllTypeConverter.AnyOrAllIterable;
import com.fortify.cli.common.rest.wait.WaitType;
import com.fortify.cli.common.rest.wait.WaitType.AnyOrAll;
import com.fortify.cli.common.rest.wait.WaitType.LoopType;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;

public final class WaitHelperWaitTypeMixin {
    @Getter @ArgGroup(exclusive = true, multiplicity = "0..1") 
    private WaitHelperWaitTypeArgGroup waitTypeArgGroup;
    
    public final WaitType getWaitType() {
        if ( waitTypeArgGroup==null ) {
            return new WaitType(LoopType.Until, AnyOrAll.all_match);
        } else if ( waitTypeArgGroup.untilAnyOrAll!=null ) { 
            return new WaitType(LoopType.Until, waitTypeArgGroup.untilAnyOrAll);
        } else {
            return new WaitType(LoopType.While, waitTypeArgGroup.whileAnyOrAll);
        }
    }
    
    private static final class WaitHelperWaitTypeArgGroup {
        @Option(names = {"--until", "-u"}, required = true, paramLabel="any-match|all-match", converter = AnyOrAllTypeConverter.class, completionCandidates = AnyOrAllIterable.class)
        @Getter private AnyOrAll untilAnyOrAll;
        @Option(names = {"--while", "-w"}, required = true, paramLabel="any-match|all-match", converter = AnyOrAllTypeConverter.class, completionCandidates = AnyOrAllIterable.class)
        @Getter private AnyOrAll whileAnyOrAll;
    }
    
    public static final class AnyOrAllTypeConverter implements ITypeConverter<AnyOrAll> {
        @Override
        public AnyOrAll convert(String value) throws Exception {
            return AnyOrAll.valueOf(value.replace('-', '_'));
        }
        
        public static final class AnyOrAllIterable extends ArrayList<String> {
            private static final long serialVersionUID = 1L;
            public AnyOrAllIterable() { 
                super(Stream.of(AnyOrAll.values())
                        .map(Enum::name)
                        .map(s->s.replace('_', '-'))
                        .collect(Collectors.toList())); 
            }
        }
    }
}