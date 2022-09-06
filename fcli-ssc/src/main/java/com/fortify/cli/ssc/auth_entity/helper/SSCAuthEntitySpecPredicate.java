package com.fortify.cli.ssc.auth_entity.helper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.util.JsonHelper;

import lombok.RequiredArgsConstructor;

/**
 * This class provides a {@link Predicate} implementation that performs
 * case-insensitive matching between auth-entity specifications (specifying 
 * auth-entity id, entityName or email) and auth-entity objects.
 *  
 * @author rsenden
 *
 */
@RequiredArgsConstructor
public final class SSCAuthEntitySpecPredicate implements Predicate<JsonNode> {
    public static enum MatchMode { INCLUDE, EXCLUDE }; 
    private final String[] authEntities;
    private final SSCAuthEntitySpecPredicate.MatchMode matchMode;
    private final boolean allowMultipleMatches;
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
        if ( !allowMultipleMatches ) {
            if ( !previousMatchedAuthEntities.add(authEntity) ) {
                throw new IllegalArgumentException(String.format("Multiple records match '%s'; please use a unique identifier or enable multiple matches", authEntity));
            }
        }
        return true;
    }
}