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

import java.io.IOException;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.Action.ActionProperties;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins.RequireConfirmation.AbortedByUserException;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.crypto.helper.SignatureHelper.InvalidSignatureHandler;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureValidator;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignedTextDescriptor;
import com.fortify.cli.common.crypto.helper.impl.SignedTextReader;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.ZipHelper;
import com.fortify.cli.common.util.ZipHelper.IZipEntryWithContextProcessor;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

public class ActionLoaderHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ActionLoaderHelper.class);
    private ActionLoaderHelper() {}
    
    public static final ActionLoadResult load(List<ActionSource> sources, String name, SignatureValidator signatureValidator) {
        return new ActionLoader(sources, signatureValidator).load(name);
    }
    
    public static final Stream<ObjectNode> streamAsJson(List<ActionSource> sources, SignatureValidator signatureValidator) {
        return _streamAsJson(sources, signatureValidator);
    }
    
    private static final Stream<ObjectNode> _streamAsJson(List<ActionSource> sources, SignatureValidator signatureValidator) {
        Map<String, ObjectNode> result = new HashMap<>();
        new ActionLoader(sources, signatureValidator)
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
    
    @RequiredArgsConstructor
    static final class ActionLoader {
        private static final SignedTextReader signedTextReader = SignatureHelper.signedTextReader();
        private final List<ActionSource> sources;
        private final SignatureValidator signatureValidator;
        
        public final ActionLoadResult load(String source) {
            // We first load from zips, in case a file happens to exist with
            // same name as an existing action, to avoid errors if for example 
            // a user saved a SARIF SAST report named 'sarif-sast-report' (with
            // no extension) in the current working directory.
            // TODO We may want to consider making this a bit smarter. For example,
            //      we may require action names to only use [a-zA-Z0-9-_]+ (also
            //      for imported actions), and only try to load from zips if source
            //      matches this regex.
            var result = loadFromZips(source);
            if ( result==null ) { result = loadFromFileOrUrl(source); }
            if ( result==null ) { throw new IllegalArgumentException("Action not found: "+source); }
            return result;
        }
        
        public final void processActions(ActionLoadResultProcessor actionLoadResultProcessor) {
            processZipEntries(zipEntryProcessor(actionLoadResultProcessor));
        }
        
        private final ActionLoadResult loadFromFileOrUrl(String source) {
            try ( var is = createSourceInputStream(source, false) ) {
                if ( is!=null ) {
                    var properties = ActionProperties.builder()
                            .custom(true).name(source).build();
                    return load(is, properties);
                }
            } catch (Exception e) {
                if ( e instanceof AbortedByUserException ) { throw (AbortedByUserException)e; }
                throw wrapException("Error loading action from "+source, e);
            }
            return null;
        }
        
        private final ActionLoadResult loadFromZips(String name) {
            try {
                AtomicReference<ActionLoadResult> result = new AtomicReference<>();
                processZipEntries(singleZipEntryProcessor(name, result::set));
                return result.get();
            } catch ( RuntimeException e ) {
                throw wrapException("Error loading action "+name, e);
            }
        }
        
        private final void processZipEntries(IZipEntryWithContextProcessor<ActionProperties> processor) {
            for ( var source: sources ) {
                var _break = ZipHelper.processZipEntries(source.getInputStreamSupplier(), 
                        processor, source.getActionProperties());
                if ( _break.doBreak() ) { break; }
            }
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
        
        final ActionLoadResult load(InputStream is, ActionProperties properties) {
            return new ActionLoadResult(loadSignedTextDescriptor(is, properties.isCustom()), properties);
        }
            
        private final SignedTextDescriptor loadSignedTextDescriptor(InputStream is, boolean isCustom) {
            return signedTextReader.load(is, StandardCharsets.UTF_8, 
                    // TODO For now, we only evaluate/check signatures for custom actions,
                    // until we've figured out how to sign internal actions during (or 
                    // potentially after) Gradle build.
                    isCustom 
                        ? signatureValidator
                        : null);
        }
        
        private final String getActionName(String fileName) {
            return Path.of(fileName).getFileName().toString().replace(".yaml", "");
        }
    }
    
    @Data
    public static final class ActionLoadResult {
        private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        private static final Pattern schemaPattern = Pattern.compile("(?m)(^\\$schema:\\s+(?<schemaPropertyValue>\\S+)\\s*$)|(^#\\s+yaml-language-server:\\s+\\$schema=(?<schemaCommentValue>\\S+)\\s*$)");
        private final SignedTextDescriptor signedTextDescriptor;
        private final ActionProperties properties;
        
        public final Action asAction() {
            try {
                var actionText = updateSchema(asText());
                var signatureStatus = signedTextDescriptor.getSignatureStatus();
                var result = yamlObjectMapper.readValue(actionText, Action.class);
                var properties = this.properties.toBuilder().signatureStatus(signatureStatus).build();
                result.postLoad(properties);
                return result;
            } catch ( Exception e ) {
                throw createException(e);
            }
        }
        
        public final String asText() {
            return signedTextDescriptor.getText();
        }
        
        public final String asRawText() {
            return signedTextDescriptor.getRawText();
        }

        public final ObjectNode asJson() {
            try {
                var payload = asText();
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
            } catch ( Exception e ) {
                throw createException(e);
            }
        }
        
        private final String updateSchema(String actionText) {
            var result = actionText;
            var matcher = schemaPattern.matcher(actionText);
            String propertyValue = null;
            String commentValue = null;
            String schemaUri = null;
            while ( matcher.find() ) {
                propertyValue = getValue("$schema", matcher.group("schemaPropertyValue"), propertyValue);
                commentValue = getValue("# yaml-language-server $schema", matcher.group("schemaCommentValue"), commentValue);
            }
            if ( StringUtils.isAllBlank(propertyValue, commentValue) ) {
                throw new IllegalStateException(getExceptionMessage("Either '$schema' property or '# yaml-language-server $schema' must be specified"));
            } else if ( StringUtils.isNoneBlank(propertyValue, commentValue) && !propertyValue.equals(commentValue) ) {
                throw new IllegalStateException(getExceptionMessage("If both '$schema' property and '# yaml-language-server $schema' are specified, the schema locations must be identical"));
            } else if ( StringUtils.isBlank(propertyValue) ) {
                result += "\n\n$schema: "+commentValue;
                schemaUri = commentValue;
            } else {
                schemaUri = propertyValue;
            }
            if ( !ActionSchemaVersionHelper.isSupportedSchemaURI(schemaUri) ) {
                LOG.warn("WARN: Action was designed for fcli version "+ActionSchemaVersionHelper.CURRENT_FCLI_VERSION+" and may fail");
            }
            return result;
        }
        
        private final String getValue(String type, String newValue, String oldValue) {
            if ( StringUtils.isBlank(oldValue) ) { 
                return newValue; 
            } else if ( StringUtils.isNotBlank(newValue) ) {
                throw new IllegalStateException(getExceptionMessage(type+" may only be specified once"));
            } else {
                return oldValue;
            }
        }
        
        private final RuntimeException createException(Exception e) {
            return wrapException(getExceptionMessage(null), e);
        }

        private String getExceptionMessage(String detailMessage) {
            var msg = "Error loading action "+properties.getName();
            if ( StringUtils.isNotBlank(detailMessage) ) { msg+=": "+detailMessage; }
            return msg;
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
        
        public static final List<ActionSource> importedActionSources(String type) {
            var result = new ArrayList<ActionSource>();
            result.add(imported(type));
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
            if ( StringUtils.isNotBlank(source) ) {
                result.add(external(source));
            }
            return result;
        }
        
        private static final ActionSource external(String source) {
            return new ActionSource(()->createSourceInputStream(source, true), ActionProperties.create(true));
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
    
    static final Path customActionsZipPath(String type) {
        return FcliDataHelper.getFcliConfigPath().resolve("action").resolve(type.toLowerCase()+".zip");
    }
    
    private static final String builtinActionsResourceZip(String type) {
        return String.format("com/fortify/cli/%s/actions.zip", type.toLowerCase().replace('-', '_'));
    }
    
    private static final String commonActionsResourceZip() {
        return "com/fortify/cli/common/actions.zip";
    }
    
    @SneakyThrows
    private static final InputStream createSourceInputStream(String source, boolean failOnError) {
        try {
            return new URL(source).openStream();
        } catch (MalformedURLException mue ) {
            try {
                return Files.newInputStream(Path.of(source));
            } catch ( IOException ioe ) {
                if ( failOnError ) {
                    throw new IllegalArgumentException("Unable to read from "+source, ioe);
                } else {
                    return null;
                }
            }
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
    
    private static final RuntimeException wrapException(String msg, Exception e) {
        if ( e!=null && e instanceof AbortedByUserException ) { return (AbortedByUserException)e; }
        return new IllegalStateException(msg, e);
    }
    
    @FunctionalInterface
    private static interface ActionLoadResultProcessor {
        Break process(ActionLoadResult loadResult);
    }
}
