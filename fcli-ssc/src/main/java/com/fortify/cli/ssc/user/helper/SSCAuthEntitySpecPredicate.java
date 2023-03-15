package com.fortify.cli.ssc.user.helper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;

import lombok.RequiredArgsConstructor;

/**
 * This class provides a {@link Predicate} implementation that performs
 * case-sensitive matching between auth-entity specifications (specifying 
 * auth-entity id, entityName or email) and auth-entity objects.
 *  
 * @author rsenden
 *
 */
@RequiredArgsConstructor
public final class SSCAuthEntitySpecPredicate implements Predicate<JsonNode> {
    private static final Logger LOG = LoggerFactory.getLogger(SSCAuthEntitySpecPredicate.class);
    public static enum MatchMode { INCLUDE, EXCLUDE }; 
    private final String[] authEntities;
    private final SSCAuthEntitySpecPredicate.MatchMode matchMode;
    private final boolean allowMultipleMatches;
    private final Set<String> previousMatchedAuthEntities = new HashSet<>();
    
    @Override
    public boolean test(JsonNode node) {
        Set<String> values = new HashSet<>(Arrays.asList( new String[]{
                getString(node, "id"),
                getString(node, "entityName"),
                getString(node, "email")
        } )); 
        boolean isMatching = 
                authEntities!=null &&
                Stream.of(authEntities)
                .filter(values::contains)
                .filter(this::hasPreviousMatch)
                .count() > 0;
        return matchMode==MatchMode.INCLUDE ? isMatching : !isMatching;
    }
    
    public String[] getUnmatched() {
        return authEntities==null 
                ? new String[] {} 
                : Stream.of(authEntities).filter(Predicate.not(previousMatchedAuthEntities::contains)).toArray(String[]::new);
    }
    
    public void checkUnmatched() {
        String[] unmatched = getUnmatched();
        if ( unmatched!=null && unmatched.length>0 ) {
            throw new IllegalArgumentException("The following auth entities cannot be found: "+String.join(", ", unmatched));
        }
    }
    
    public void logUnmatched(String msg) {
        String[] unmatched = getUnmatched();
        if ( unmatched!=null && unmatched.length>0 ) {
            LOG.warn(msg+String.join(", ", unmatched));
        }
    }

    private String getString(JsonNode node, String field) {
        return JsonHelper.evaluateJsonPath(node, field, String.class);
    }
    
    private boolean hasPreviousMatch(String authEntity) {
        if ( !previousMatchedAuthEntities.add(authEntity) && !allowMultipleMatches ) {
            throw new IllegalArgumentException(String.format("Multiple records match '%s'; please use a unique identifier or enable multiple matches", authEntity));
        }
        return true;
    }
}