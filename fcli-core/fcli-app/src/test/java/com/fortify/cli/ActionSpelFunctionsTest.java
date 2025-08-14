/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.spel.fn.descriptor.SpelFunctionDescriptorsFactory;
import com.fortify.cli.common.spel.fn.descriptor.SpelFunctionDescriptorsFactory.SpelFunctionDescriptor;

/**
 *
 * @author Ruud Senden
 */
public class ActionSpelFunctionsTest {
    @Test
    public void testDuplicateSpelFunctionNames() {
        Map<String, List<SpelFunctionDescriptor>> byName = SpelFunctionDescriptorsFactory
                .getActionSpelFunctionsDescriptors().stream()
                .collect(Collectors.groupingBy(SpelFunctionDescriptor::getName));
            List<String> duplicateMessages = byName.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> {
                    String functionName = e.getKey();
                    String classNames = e.getValue().stream()
                        .map(f -> f.getClazz().getName())
                        .distinct()
                        .collect(Collectors.joining(", "));
                    return String.format("Duplicate function name '%s' found in classes: %s", functionName, classNames);
                })
                .collect(Collectors.toList());
            if ( !duplicateMessages.isEmpty() ) {
                var msg = String.join("\n", duplicateMessages);
                System.err.println(msg);
                throw new IllegalStateException(msg);
            }
    }
}
