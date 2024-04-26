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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.Action.ActionProperties;
import com.fortify.cli.common.crypto.SignatureHelper;
import com.fortify.cli.common.crypto.SignatureHelper.InvalidSignatureHandler;
import com.fortify.cli.common.crypto.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.crypto.SignatureHelper.SignedTextDescriptor;
import com.fortify.cli.common.crypto.impl.SignedTextReader;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.ZipHelper;
import com.fortify.cli.common.util.ZipHelper.IZipEntryWithContextProcessor;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

public class ActionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ActionHelper.class);
    private ActionHelper() {}
    
    public static final Action loadAction(String type, String name, InvalidSignatureHandler invalidSignatureHandler) {
        return load(ActionSource.defaultActionSources(type), name, invalidSignatureHandler).asAction();
    }
    
    public static final Action loadAction(List<ActionSource> sources, String name, InvalidSignatureHandler invalidSignatureHandler) {
        return load(sources, name, invalidSignatureHandler).asAction();
    }

    public static final String loadActionContents(String type, String name, InvalidSignatureHandler invalidSignatureHandler) {
        return load(ActionSource.defaultActionSources(type), name, invalidSignatureHandler).asString();
    }
    
    public static final String loadActionContents(List<ActionSource> sources, String name, InvalidSignatureHandler invalidSignatureHandler) {
        return load(sources, name, invalidSignatureHandler).asString();
    }
    
    public static final ActionLoadResult load(List<ActionSource> sources, String name, InvalidSignatureHandler invalidSignatureHandler) {
        return new ActionLoader(sources, invalidSignatureHandler).load(name);
    }
    
    public static final Stream<ObjectNode> streamAsJson(String type, InvalidSignatureHandler invalidSignatureHandler) {
        return _streamAsJson(ActionSource.defaultActionSources(type), invalidSignatureHandler);
    }
    
    public static final Stream<ObjectNode> streamAsJson(List<ActionSource> sources, InvalidSignatureHandler invalidSignatureHandler) {
        return _streamAsJson(sources, invalidSignatureHandler);
    }
    
    private static final Stream<ObjectNode> _streamAsJson(List<ActionSource> sources, InvalidSignatureHandler invalidSignatureHandler) {
        Map<String, ObjectNode> result = new HashMap<>();
        new ActionLoader(sources, invalidSignatureHandler)
            .processActions(loadResult->{
                result.putIfAbsent(loadResult.getProperties().getName(), loadResult.asJson());
                return Break.FALSE;
            });
        return result.values().stream()
                .sorted((a,b)->a.get("name").asText().compareTo(b.get("name").asText()));
    }
    
    public static final String getSignatureStatusMessage(SignatureStatus signatureStatus) {
        switch (signatureStatus) {
        case INVALID_SIGNATURE: return "Action signature is invalid.";
        case NO_PUBLIC_KEY: return "No trusted public key found to verify action signature.";
        case NO_SIGNATURE: return "Action is not signed.";
        case NOT_VERIFIED: return "Action signature verification skipped.";
        case VALID_SIGNATURE: return "Action has a valid signature.";
        default: throw new RuntimeException("Unknown signature status: "+signatureStatus);
        }
    }
    
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
        var result = JsonHelper.getObjectMapper().createArrayNode();
        var zipPath = customActionsZipPath(type);
        new ActionLoader(ActionSource.defaultActionSources(type), ActionInvalidSignatureHandlers.IGNORE)
            .processZipEntries(zipPath, ActionProperties.create(true),
                        loadResult->{
                            result.add(loadResult.asJson());
                            return Break.FALSE;});
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
    */

    /*
    @SneakyThrows
    private static void importEntry(String name, String type, String contents) {
        Map<String, String> env = Collections.singletonMap("create", "true");

        try (FileSystem zipfs = FileSystems.newFileSystem(getCustomActionsZipPath(type), env)) {
          Path filePath = zipfs.getPath(getActionName(name)+".yaml");
          Files.write(filePath, contents.getBytes(StandardCharsets.US_ASCII), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    */

    @RequiredArgsConstructor
    private static final class ActionLoader {
        private static final SignedTextReader signedTextReader = SignatureHelper.signedTextReader();
        private final List<ActionSource> sources;
        private final InvalidSignatureHandler invalidSignatureHandler;
        
        public final ActionLoadResult load(String name) {
            AtomicReference<ActionLoadResult> result = new AtomicReference<>();
            processZipEntries(singleZipEntryProcessor(name, result::set));
            if ( result.get()==null ) {
                throw new IllegalArgumentException("No action found with name "+name);
            }
            return result.get();
        }
        
        public final void processActions(ActionLoadResultProcessor actionLoadResultProcessor) {
            processZipEntries(zipEntryProcessor(actionLoadResultProcessor));
        }
        
        private final void processZipEntries(IZipEntryWithContextProcessor<ActionProperties> processor) {
            for ( var source: sources ) {
                var _break = ZipHelper.processZipEntries(source.getInputStreamSupplier(), 
                        processor, source.getActionProperties());
                if ( _break.doBreak() ) { break; }
            }
        }
        
        public final Break processZipEntries(Path zipFilePath, ActionProperties properties, ActionLoadResultProcessor loadResultProcessor) {
            return ZipHelper.processZipEntries(()->FileUtils.getInputStream(zipFilePath), zipEntryProcessor(loadResultProcessor), properties);
        }
        
        public final Break processZipEntries(Supplier<InputStream> zipFileInputStreamSupplier, ActionProperties properties, ActionLoadResultProcessor loadResultProcessor) {
            return ZipHelper.processZipEntries(zipFileInputStreamSupplier, zipEntryProcessor(loadResultProcessor), properties);
        }
        
        public final Break processZipEntries(InputStream zipFileInputStream, ActionProperties properties, ActionLoadResultProcessor loadResultProcessor) {
            return ZipHelper.processZipEntries(zipFileInputStream, zipEntryProcessor(loadResultProcessor), properties);
        }
        
        private final IZipEntryWithContextProcessor<ActionProperties> zipEntryProcessor(ActionLoadResultProcessor loadResultProcessor) {
            return (zis, ze, properties) -> loadResultProcessor.process(load(zis, ze, properties)); 
        }
        
        private final IZipEntryWithContextProcessor<ActionProperties> singleZipEntryProcessor(String name, Consumer<ActionLoadResult> loadResultConsumer) {
            return (zis, ze, properties) -> processSingleZipEntry(zis, ze, name, loadResultConsumer, properties);
        }

        private Break processSingleZipEntry(ZipInputStream zis, ZipEntry ze, String name, Consumer<ActionLoadResult> loadResultConsumer, ActionProperties properties) {
            var fileName = name+".yaml";
            if (ze.getName().equals(fileName)) {
                loadResultConsumer.accept(load(zis, ze, properties));
                return Break.TRUE;
            }
            return Break.FALSE;
        }
        
        private final ActionLoadResult load(ZipInputStream zis, ZipEntry ze, ActionProperties properties) {
            properties = properties.toBuilder().name(getActionName(ze.getName())).build();
            return load(zis, properties);
        }
        
        private final ActionLoadResult load(InputStream is, ActionProperties properties) {
            return new ActionLoadResult(loadSignedTextDescriptor(is, properties.isCustom()), properties);
        }
            
        private final SignedTextDescriptor loadSignedTextDescriptor(InputStream is, boolean isCustom) {
            return signedTextReader.load(is, StandardCharsets.UTF_8, 
                    // TODO For now, we only evaluate/check signatures for custom actions,
                    // until we've figured out how to sign internal actions during (or 
                    // potentially after) Gradle build.
                    isCustom 
                        ? invalidSignatureHandler
                        : null);
        }
        
        private final String getActionName(String fileName) {
            return Path.of(fileName).getFileName().toString().replace(".yaml", "");
        }
    }
    
    @Data
    private static final class ActionLoadResult {
        private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        private final SignedTextDescriptor signedTextDescriptor;
        private final ActionProperties properties;
        
        @SneakyThrows
        final Action asAction() {
            var payload = signedTextDescriptor.getPayload();
            var signatureStatus = signedTextDescriptor.getSignatureStatus();
            var result = yamlObjectMapper.readValue(payload, Action.class);
            var properties = this.properties.toBuilder().signatureStatus(signatureStatus).build();
            result.postLoad(properties);
            return result;
        }
        
        public String asString() {
            return signedTextDescriptor.getPayload();
        }

        @SneakyThrows
        final ObjectNode asJson() {
            var payload = signedTextDescriptor.getPayload();
            var signatureStatus = signedTextDescriptor.getSignatureStatus();
            String name = properties.getName();
            boolean custom = properties.isCustom();
            var customString = custom?"Yes":"No";
            // TODO see ActionLoader#loadSignedTextDescriptor; for internal actions
            // we currently don't evaluate signatures until we implement functionality
            // for signing these during or after build.
            var signatureString = !custom || signatureStatus==SignatureStatus.VALID_SIGNATURE 
                ? "Valid" : "Invalid";
            return yamlObjectMapper.readValue(payload, ObjectNode.class)
                    .put("name", name)
                    .put("custom", custom)
                    .put("customString", customString)
                    .put("signatureStatus", signatureStatus.toString())
                    .put("signatureString", signatureString);
        }
    }
    
    @Data @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ActionSource {
        private final Supplier<InputStream> inputStreamSupplier;
        private final ActionProperties actionProperties;
        
        public static final List<ActionSource> defaultActionSources(String type) {
            var result = new ArrayList<ActionSource>();
            result.add(imported(type));
            result.add(builtin(type));
            result.add(common(type));
            return result;
        }
        
        public static final List<ActionSource> builtinActionSources(String type) {
            var result = new ArrayList<ActionSource>();
            result.add(builtin(type));
            result.add(common(type));
            return result;
        }
        
        public static final List<ActionSource> externalActionSources(String source) {
            var result = new ArrayList<ActionSource>();
            result.add(external(source));
            return result;
        }
        
        private static final ActionSource external(String source) {
            return new ActionSource(()->createURLOrFileInputStream(source), ActionProperties.create(true));
        }
        
        private static final ActionSource imported(String type) {
            return new ActionSource(customActionsInputStreamSupplier(type), ActionProperties.create(true));
        }
        
        private static final ActionSource builtin(String type) {
            return new ActionSource(builtinActionsInputStreamSupplier(type), ActionProperties.create(false));
        }
        
        private static final ActionSource common(String type) {
            return new ActionSource(commonActionsInputStreamSupplier(), ActionProperties.create(false));
        }
        
        @SneakyThrows
        private static final Supplier<InputStream> customActionsInputStreamSupplier(String type) {
            return ()->FileUtils.getInputStream(customActionsZipPath(type));
        }
        
        private static final Supplier<InputStream> builtinActionsInputStreamSupplier(String type) {
            return ()->FileUtils.getResourceInputStream(builtinActionsResourceZip(type));
        }
        
        private static final Supplier<InputStream> commonActionsInputStreamSupplier() {
            return ()->FileUtils.getResourceInputStream(commonActionsResourceZip());
        }
    }
    
    private static final Path customActionsZipPath(String type) {
        return FcliDataHelper.getFcliConfigPath().resolve("action").resolve(type.toLowerCase()+".zip");
    }
    
    private static final String builtinActionsResourceZip(String type) {
        return String.format("com/fortify/cli/%s/actions.zip", type.toLowerCase().replace('-', '_'));
    }
    
    private static final String commonActionsResourceZip() {
        return "com/fortify/cli/common/actions.zip";
    }
    
    @SneakyThrows
    private static final InputStream createURLOrFileInputStream(String source) {
        try {
            return new URL(source).openStream();
        } catch (MalformedURLException e ) {
            return Files.newInputStream(Path.of(source));
        }
    }
    
    @RequiredArgsConstructor
    public static final class ActionInvalidSignatureHandlers {
        public static final InvalidSignatureHandler IGNORE   = InvalidSignatureHandler.IGNORE;
        public static final InvalidSignatureHandler EVALUATE = InvalidSignatureHandler.EVALUATE;
        public static final InvalidSignatureHandler WARN     = ActionInvalidSignatureHandlers::warn;
        public static final InvalidSignatureHandler FAIL     = ActionInvalidSignatureHandlers::fail;
        
        private static final void warn(SignedTextDescriptor descriptor) {
            LOG.warn("WARN: "+failedMessage(descriptor));
        }
        private static final void fail(SignedTextDescriptor descriptor) {
            throw new IllegalStateException(failedMessage(descriptor));
        }
        private static final String failedMessage(SignedTextDescriptor descriptor) {
            return getSignatureStatusMessage(descriptor.getSignatureStatus());
        }
    }
    
    @FunctionalInterface
    private static interface ActionLoadResultProcessor {
        Break process(ActionLoadResult loadResult);
    }
}
