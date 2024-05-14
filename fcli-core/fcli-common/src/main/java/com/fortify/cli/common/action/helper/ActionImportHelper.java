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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionLoadResult;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionLoader;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.Action.ActionProperties;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.common.util.ZipHelper;

import lombok.SneakyThrows;

public class ActionImportHelper {
    @SneakyThrows
    public static ArrayNode importAction(String type, String externalSource, String action, ActionValidationHandler actionValidationHandler) {
        var result = JsonHelper.getObjectMapper().createArrayNode();
        try ( var fs = createOutputZipFileSystem(type) ) {
            var actionLoadResult = new ActionLoader(ActionSource.externalActionSources(externalSource), actionValidationHandler)
                    .load(action);
            result.add(importAction(fs, actionLoadResult));
        }
        return result;
    }

    @SneakyThrows
    public static ArrayNode importZip(String type, String zip, ActionValidationHandler actionValidationHandler) {
        var result = JsonHelper.getObjectMapper().createArrayNode();
        var loader = new ActionLoader(null, actionValidationHandler);
        try ( var fs = createOutputZipFileSystem(type); var is = createZipFileInputStream(zip) ) {
            ZipHelper.processZipEntries(is, (zis, entry)->
                importAction(fs, result, loader, zis, entry));
        }
        return result;
    }

    @SneakyThrows
    private static final InputStream createZipFileInputStream(String zip) {
        try {
            return new URL(zip).openStream();
        } catch ( MalformedURLException e ) {
            return Files.newInputStream(Path.of(zip));
        }
    }

    @SneakyThrows
    private static final FileSystem createOutputZipFileSystem(String type) {
        Map<String, String> env = Collections.singletonMap("create", "true");
        var zipPath = ActionLoaderHelper.customActionsZipPath(type);
        return FileSystems.newFileSystem(zipPath, env);
    }
    
    private static final Break importAction(FileSystem fs, ArrayNode result, ActionLoader loader, ZipInputStream zis, ZipEntry entry) {
        var properties = ActionProperties.builder()
                .custom(true).name(entry.getName()).build();
        try {
            result.add(importAction(fs, loader.load(zis, properties)));
        } catch ( Exception e ) {
            result.add(createErrorEntry(properties));
        }
        return Break.FALSE;  
    }
    
    private static JsonNode createErrorEntry(ActionProperties properties) {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("name", properties.getName())
                .put(IActionCommandResultSupplier.actionFieldName, "ERROR");
    }

    @SneakyThrows
    private static final ObjectNode importAction(FileSystem fs, ActionLoadResult actionLoadResult) {
        var action = actionLoadResult.asAction(); // Validate action and allow for retrieving name
        var contents = actionLoadResult.asRawText();
        var path = fs.getPath(getTargetFileName(action));
        Files.write(path, contents.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        var result = actionLoadResult.asJson();
        return result.put("name", cleanActionName(path.getFileName().toString()));
    }

    private static final String cleanActionName(String name) {
        return name.replaceAll("\\.([^.]*)$", "[.$1]");
    }

    private static final String getTargetFileName(Action action) {
        var path = action.getName(); // May be simple name, path or URL
        // TODO May be can use URI instead, to handle both URLs and local files?
        try {
            path = new URL(path).getPath();
        } catch ( MalformedURLException e) {}
        if ( !path.endsWith(".yaml") ) { path+=".yaml"; }
        return Path.of(path).getFileName().toString();
    }
    
    @SneakyThrows
    public static final ArrayNode reset(String type) {
        var zipPath = ActionLoaderHelper.customActionsZipPath(type);
        if ( !Files.exists(zipPath) ) {
            return JsonHelper.getObjectMapper().createArrayNode();
        } else {
            var result = ActionLoaderHelper
                    .streamAsJson(ActionSource.importedActionSources(type), ActionValidationHandler.IGNORE)
                    .collect(JsonHelper.arrayNodeCollector());
            Files.delete(zipPath);
            return result;
        }
    }
}
