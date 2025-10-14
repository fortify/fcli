package com.fortify.cli.aviator.fpr.processor;

import com.fortify.cli.aviator.fpr.jaxb.Snippets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processor for Snippets section in FVDL. Caches snippet ID to text for use in traces.
 */
public class SnippetProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SnippetProcessor.class);
    private final Map<String, String> snippetMap = new ConcurrentHashMap<>();

    /**
     * Processes Snippets section and populates snippetMap.
     *
     * @param snippets JAXB Snippets object
     */
    public void process(Snippets snippets) {
        if (snippets == null || snippets.getSnippet() == null) {
            logger.debug("No Snippets or empty Snippet list");
            return;
        }

        for (Snippets.Snippet snippet : snippets.getSnippet()) {
            if (snippet.getId() != null && snippet.getText() != null) {
                snippetMap.put(snippet.getId().toString(), snippet.getText());
            } else {
                logger.warn("Invalid Snippet: missing ID or Text");
            }
        }
    }

    /**
     * Gets the cached snippet map.
     *
     * @return Map of snippet ID to text
     */
    public Map<String, String> getSnippetMap() {
        return snippetMap;
    }

    /**
     * Retrieves snippet text by ID.
     *
     * @param id Snippet ID
     * @return Snippet text or empty string if not found
     */
    public String getSnippet(String id) {
        return snippetMap.getOrDefault(id, "");
    }
}