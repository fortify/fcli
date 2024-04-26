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
package com.fortify.cli.common.action.helper;

import java.nio.file.Files;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionInvalidSignatureHandlers;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.json.JsonHelper;

import lombok.SneakyThrows;

public class ActionImportHelper {
    /*
    @SneakyThrows
    public static final ArrayNode importZip(String type, String source) {
        var result = JsonHelper.getObjectMapper().createArrayNode();
        IZipEntryProcessor<Boolean> processor = (zis, ze, isCustom)->importEntry(result, zis, getActionName(ze.getName()), type);
        try {
            var url = new URL(source);
            try ( var unirest = createUnirestInstance(type, url) ) {
                unirest.get(url.toString()).asObject(r->processZipEntries(r.getContent(), processor, true)).getBody();
            }
        } catch (MalformedURLException e ) {
            try ( var is = Files.newInputStream(Path.of(source)) ) {
                processZipEntries(is, processor, true);
            }
        }
        return result;
    }
    */
    
    /*
    @SneakyThrows
    public static final ArrayNode importSingle(String type, String name, String source) {
        var result = JsonHelper.getObjectMapper().createArrayNode();
        var finalName = StringUtils.isBlank(name) ? getActionName(source) : name;
        try {
            var url = new URL(source);
            try ( var unirest = createUnirestInstance(type, url) ) {
                unirest.get(url.toString()).asObject(r->importEntry(result, r.getContent(), finalName, type)).getBody();
            }
        } catch (MalformedURLException e ) {
            try ( var is = Files.newInputStream(Path.of(source)) ) {
                importEntry(result, is, finalName, type);
            }
        }
        return result;
    }
    
    */
    @SneakyThrows
    public static final ArrayNode reset(String type) {
        var zipPath = ActionLoaderHelper.customActionsZipPath(type);
        var result = ActionLoaderHelper
            .streamAsJson(ActionSource.importedActionSources(type), ActionInvalidSignatureHandlers.IGNORE)
            .collect(JsonHelper.arrayNodeCollector());
        Files.delete(zipPath);
        return result;
    }
    /*
    
    private static final boolean importEntry(ArrayNode importedEntries, InputStream is, String name, String type) {
        try {
            // Read input stream as string for further processing
            var contents = FileUtils.readInputStreamAsString(is, StandardCharsets.US_ASCII);
            // Read contents as JsonNode to update importedEntries array after import
            var json = yamlObjectMapper.readValue(contents, ObjectNode.class);
            // Verify that the template can be successfully parsed and post-processed
            yamlObjectMapper.treeToValue(json, Action.class).postLoad(name, true);
            // Import entry to zip-file
            importEntry(name, type, contents);
            // Add JSON to the imported entries array.
            importedEntries.add(updateJson(name, true, json));
        } catch ( Exception e ) {
            LOG.warn("WARN: Skipping "+name+" due to errors, see debug log for details");
            LOG.debug("WARN: Skipping "+name+" due to errors", e);
        }
        return true;
    }
    
    @SneakyThrows
    private static void importEntry(String name, String type, String contents) {
        Map<String, String> env = Collections.singletonMap("create", "true");

        try (FileSystem zipfs = FileSystems.newFileSystem(getCustomActionsZipPath(type), env)) {
          Path filePath = zipfs.getPath(getActionName(name)+".yaml");
          Files.write(filePath, contents.getBytes(StandardCharsets.US_ASCII), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    */
}
