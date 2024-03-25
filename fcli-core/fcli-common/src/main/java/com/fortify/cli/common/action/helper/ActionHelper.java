/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.action.helper;

import java.io.IOException;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;

public class ActionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ActionHelper.class);
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    private ActionHelper() {}
    
    public static final ActionDescriptor load(String type, String name) {
        return loadZipEntry(type, name, ActionHelper::loadDescriptor);
    }

    public static final String loadContents(String type, String name) {
        return loadZipEntry(type, name, (is, ze, isCustom)->FileUtils.readInputStreamAsString(is, StandardCharsets.US_ASCII));
    }
    
    public static final Stream<ObjectNode> list(String type) {
        Map<String, ObjectNode> result = new HashMap<>();
        processBuiltinAndCustomZipEntries(type, (zis, ze, isCustom)->{
            result.putIfAbsent(getActionName(ze.getName()), loadAsJson(ze.getName(), zis, isCustom));
            return true;
        });
        return result.values().stream()
                .sorted((a,b)->a.get("name").asText().compareTo(b.get("name").asText()));
    }
    
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
    
    @SneakyThrows
    public static final ArrayNode reset(String type) {
        var result = JsonHelper.getObjectMapper().createArrayNode();
        var zipPath = getCustomActionsZipPath(type);
        if ( Files.exists(zipPath) ) {
            try ( var is = Files.newInputStream(zipPath) ) {
                processZipEntries(is, (zis, ze, isCustom) -> {
                    result.add(loadAsJson(ze.getName(), zis, isCustom));
                    return true;
                }, true);
            }
            Files.delete(zipPath);
        }
        return result;
    }
    
    
    private static final boolean importEntry(ArrayNode importedEntries, InputStream is, String name, String type) {
        try {
            // Read input stream as string for further processing
            var contents = FileUtils.readInputStreamAsString(is, StandardCharsets.US_ASCII);
            // Read contents as JsonNode to update importedEntries array after import
            var json = yamlObjectMapper.readValue(contents, ObjectNode.class);
            // Verify that the template can be successfully parsed and post-processed
            yamlObjectMapper.treeToValue(json, ActionDescriptor.class).postLoad(name, true);
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

    private static final UnirestInstance createUnirestInstance(String type, URL url) {
        var result = GenericUnirestFactory.createUnirestInstance(); 
        ProxyHelper.configureProxy(result, type.toLowerCase()+"-action", url.toString());
        return result;
    }
    
    private static final ActionDescriptor loadDescriptor(InputStream is, ZipEntry ze, boolean isCustom) {
        return loadDescriptor(is, ze.getName(), isCustom);
    }

    private static final ActionDescriptor loadDescriptor(InputStream is, String actionName, boolean isCustom) {
        try {
            var result = yamlObjectMapper.readValue(is, ActionDescriptor.class);
            result.postLoad(getActionName(actionName), isCustom);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Error loading action "+actionName, e);
        }
    }
    
    private static final ObjectNode loadAsJson(String fileName, InputStream is, boolean isCustom) {
        if ( is==null ) {
            // TODO Use more descriptive exception message
            throw new IllegalStateException("Can't read "+fileName);
        }
        try {
            var result = yamlObjectMapper.readValue(is.readAllBytes(), ObjectNode.class); 
            return updateJson(fileName, isCustom, result);
        } catch (IOException e) {
            throw new RuntimeException("Error loading action "+fileName, e);
        }
    }

    private static ObjectNode updateJson(String fileName, boolean isCustom, ObjectNode result) {
        return result
            .put("name", getActionName(fileName))
            .put("custom", isCustom)
            .put("isCustomString", isCustom?"Yes":"No");
    }
    
    private static final String getActionName(String fileName) {
        return Path.of(fileName).getFileName().toString().replace(".yaml", "");
    }
    
    private static final <T> T loadZipEntry(String type, String name, IZipEntryProcessor<T> processor) {
        AtomicReference<T> result = new AtomicReference<>();
        processBuiltinAndCustomZipEntries(type, (zis, ze, isCustom)->{
            var fileName = name+".yaml";
            if (ze.getName().equals(fileName)) {
                result.set(processor.process(zis, ze, isCustom));
                return false;
            } else {
                return true;
            }
        });
        if ( result.get()==null ) {
            throw new IllegalArgumentException("No action found with name "+name);
        }
        return result.get();
    }
    
    @SneakyThrows
    private static final void processBuiltinAndCustomZipEntries(String type, IZipEntryProcessor<Boolean> processor) {
        boolean _continue;
        try ( var customActionsZipFileInputStream = getCustomActionsZipFileInputStream(type) ) {
            _continue = processZipEntries(customActionsZipFileInputStream, processor, true);
        }
        if ( _continue ) {
            try ( var builtinActionsZipFileInputStream = getBuiltinActionsZipFileInputStream(type) ) {
                _continue = processZipEntries(builtinActionsZipFileInputStream, processor, false);
            } 
        }
        if ( _continue ) {
            try ( var commonActionsZipFileInputStream = getCommonActionsZipFileInputStream() ) {
                _continue = processZipEntries(commonActionsZipFileInputStream, processor, false);
            } 
        }
    }
    
    private static final boolean processZipEntries(InputStream zipFileInputStream, IZipEntryProcessor<Boolean> processor, boolean isCustom) {
        if ( zipFileInputStream!=null ) {
            try ( ZipInputStream zis = new ZipInputStream(zipFileInputStream) ) {
                ZipEntry entry;
                while ( (entry = zis.getNextEntry())!=null ) {
                    if ( !processor.process(zis, entry, isCustom) ) { return false; }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error loading actions", e);
            }
        }
        return true;
    }
    
    @SneakyThrows
    private static final InputStream getCustomActionsZipFileInputStream(String type) {
        var zipPath = getCustomActionsZipPath(type);
        return !Files.exists(zipPath) ? null : Files.newInputStream(zipPath);
    }
    
    private static final InputStream getBuiltinActionsZipFileInputStream(String type) {
        return FileUtils.getResourceInputStream(getBuiltinActionsResourceZip(type));
    }
    
    private static final InputStream getCommonActionsZipFileInputStream() {
        return FileUtils.getResourceInputStream(getCommonActionsResourceZip());
    }
    
    private static Path getCustomActionsZipPath(String type) {
        return FcliDataHelper.getFcliConfigPath().resolve("action").resolve(type.toLowerCase()+".zip");
    }
    
    private static final String getBuiltinActionsResourceZip(String type) {
        return String.format("com/fortify/cli/%s/actions.zip", type.toLowerCase().replace('-', '_'));
    }
    
    private static final String getCommonActionsResourceZip() {
        return "com/fortify/cli/common/actions.zip";
    }
    
    @FunctionalInterface
    private static interface IZipEntryProcessor<T> {
        T process(ZipInputStream is, ZipEntry entry, boolean isCustom);
    }
}
