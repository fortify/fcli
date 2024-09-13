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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
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
import com.fortify.cli.common.action.model.Action.ActionMetadata;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins.RequireConfirmation.AbortedByUserException;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeyDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeySource;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureMetadata;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureValidator;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignedTextDescriptor;
import com.fortify.cli.common.crypto.helper.impl.SignedTextReader;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.ZipHelper;
import com.fortify.cli.common.util.ZipHelper.IZipEntryWithContextProcessor;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.SneakyThrows;

public class ActionLoaderHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ActionLoaderHelper.class);
    private ActionLoaderHelper() {}
    
    public static final ActionLoadResult load(List<ActionSource> sources, String name, ActionValidationHandler actionValidationHandler) {
        return new ActionLoader(sources, actionValidationHandler).load(name);
    }
    
    public static final Stream<Action> streamAsActions(List<ActionSource> sources, ActionValidationHandler actionValidationHandler) {
        return _stream(sources, actionValidationHandler, ActionLoadResult::getAction, a->a.getMetadata().getName());
    }
    
    public static final Stream<ObjectNode> streamAsJson(List<ActionSource> sources, ActionValidationHandler actionValidationHandler) {
        return _stream(sources, actionValidationHandler, ActionLoadResult::getSummaryObjectNode, o->o.get("name").asText());
    }
    
    private static final <T> Stream<T> _stream(List<ActionSource> sources, ActionValidationHandler actionValidationHandler, Function<ActionLoadResult, T> asTypeFunction, Function<T, String> nameFunction) {
        Map<String, T> result = new HashMap<>();
        new ActionLoader(sources, actionValidationHandler)
            .processActions(loadResult->{
                result.putIfAbsent(loadResult.getMetadata().getName(), asTypeFunction.apply(loadResult));
                return Break.FALSE;
            });
        return result.values().stream()
                .sorted((a,b)->nameFunction.apply(a).compareTo(nameFunction.apply(b)));
    }
    
    public static final String getSignatureStatusMessage(ActionMetadata metadata, SignatureStatus signatureStatus) {
        var name = metadata.getName();
        switch (signatureStatus) {
        case MISMATCH: return "Signature for action "+name+" is invalid.";
        case NO_PUBLIC_KEY: return "No trusted public key found to verify "+name+" action signature.";
        case UNSIGNED: return "Action "+name+" is not signed.";
        case NOT_VERIFIED: return "Signature verification skipped for action "+name+".";
        case VALID: return "Action "+name+" has a valid signature.";
        default: throw new RuntimeException("Unknown signature status: "+signatureStatus);
        }
    }
    
    @RequiredArgsConstructor
    static final class ActionLoader {
        private static final SignedTextReader signedTextReader = SignatureHelper.signedTextReader();
        private final List<ActionSource> sources;
        private final ActionValidationHandler actionValidationHandler;
        
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
                    var metadata = ActionMetadata.builder()
                            .custom(true).name(source).build();
                    return load(is, metadata);
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
        
        private final void processZipEntries(IZipEntryWithContextProcessor<ActionMetadata> processor) {
            for ( var source: sources ) {
                var _break = ZipHelper.processZipEntries(source.getInputStreamSupplier(), 
                        processor, source.getMetadata());
                if ( _break.doBreak() ) { break; }
            }
        }
        
        private final IZipEntryWithContextProcessor<ActionMetadata> zipEntryProcessor(ActionLoadResultProcessor loadResultProcessor) {
            return (zis, ze, metadata) -> loadResultProcessor.process(load(zis, ze, metadata)); 
        }
        
        private final IZipEntryWithContextProcessor<ActionMetadata> singleZipEntryProcessor(String name, Consumer<ActionLoadResult> loadResultConsumer) {
            return (zis, ze, metadata) -> processSingleZipEntry(zis, ze, name, loadResultConsumer, metadata);
        }

        private Break processSingleZipEntry(ZipInputStream zis, ZipEntry ze, String name, Consumer<ActionLoadResult> loadResultConsumer, ActionMetadata metadata) {
            var fileName = name+".yaml";
            if (ze.getName().equals(fileName)) {
                loadResultConsumer.accept(load(zis, ze, metadata));
                return Break.TRUE;
            }
            return Break.FALSE;
        }
        
        private final ActionLoadResult load(ZipInputStream zis, ZipEntry ze, ActionMetadata metadata) {
            metadata = metadata.toBuilder().name(getActionName(ze.getName())).build();
            return load(zis, metadata);
        }
        
        final ActionLoadResult load(InputStream is, ActionMetadata metadata) {
            return new ActionLoadResult(actionValidationHandler, loadSignedTextDescriptor(metadata, is), metadata);
        }
            
        private final SignedTextDescriptor loadSignedTextDescriptor(ActionMetadata metadata, InputStream is) {
            return signedTextReader.load(is, StandardCharsets.UTF_8, 
                    // TODO For now, we only evaluate/check signatures for custom actions,
                    // until we've figured out how to sign internal actions during (or 
                    // potentially after) Gradle build.
                    metadata.isCustom() 
                        ? actionValidationHandler.getSignatureValidator(metadata)
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
        private final ActionValidationHandler actionValidationHandler;
        private final SignedTextDescriptor signedTextDescriptor;
        private final ActionMetadata metadata;
        
        ActionLoadResult(ActionValidationHandler actionValidationHandler, SignedTextDescriptor signedTextDescriptor, ActionMetadata metadata) {
            this.actionValidationHandler = actionValidationHandler;
            this.signedTextDescriptor = signedTextDescriptor;
            this.metadata = updateMetadata(metadata, signedTextDescriptor);
        }
        
        /**
         * @return Deserialized and initialized {@link Action} instance.
         */
        public final Action getAction() {
            try {
                checkSchema();
                var result = yamlObjectMapper.readValue(getActionText(), Action.class);
                result.postLoad(metadata);
                return result;
            } catch ( Exception e ) {
                throw createException(e);
            }
        }
        
        /**
         * @return Textual action contents.
         */
        public final String getActionText() {
            return signedTextDescriptor.getPayload();
        }
        
        /**
         * @return Original action file contents, including
         *         signature if available.
         */
        public final String getOriginalText() {
            return signedTextDescriptor.getOriginal();
        }
        
        public final ActionSummaryDescriptor getSummaryDescriptor() {
            return ActionSummaryDescriptor.fromActionLoadResult(this);
        }
        
        public final ObjectNode getSummaryObjectNode() {
            return getSummaryDescriptor().asObjectNode();
        }
        
        private static final ActionMetadata updateMetadata(ActionMetadata metadata, SignedTextDescriptor signedTextDescriptor) {
            var custom = metadata.isCustom();
            return metadata.toBuilder()
                    .signatureDescriptor(getSignatureDescriptor(custom, signedTextDescriptor))
                    .signatureStatus(getSignatureStatus(custom, signedTextDescriptor))
                    .publicKeyDescriptor(getPublicKeyDescriptor(custom, signedTextDescriptor))
                    .build();
        }
        
        private static SignatureDescriptor getSignatureDescriptor(boolean custom, SignedTextDescriptor signedTextDescriptor) {
            return custom 
                ? signedTextDescriptor.getSignatureDescriptor()
                : SignatureDescriptor.builder()
                    .signature("N/A")
                    .publicKeyFingerprint(SignatureHelper.fortifySignatureVerifier().publicKeyFingerPrint())
                    .metadata(SignatureMetadata.builder()
                            .fcliVersion(FcliBuildPropertiesHelper.getFcliVersion())
                            .signer("Fortify").build()).build();
        }
        
        private static SignatureStatus getSignatureStatus(boolean custom, SignedTextDescriptor signedTextDescriptor) {
            return custom
                    ? signedTextDescriptor.getSignatureStatus()
                    : SignatureStatus.VALID;
        }
        
        private static PublicKeyDescriptor getPublicKeyDescriptor(boolean custom, SignedTextDescriptor signedTextDescriptor) {
            return custom
                    ? signedTextDescriptor.getPublicKeyDescriptor()
                    : PublicKeyDescriptor.builder()
                        .fingerprint(SignatureHelper.fortifySignatureVerifier().publicKeyFingerPrint())
                        .name("Fortify")
                        .publicKey(SignatureHelper.FORTIFY_PUBLIC_KEY)
                        .source(PublicKeySource.INTERNAL)
                        .build();
        }
        
        private final void checkSchema() {
            var schemaUri = getSchemaUri();
            var schemaVersion = ActionSchemaHelper.getSchemaVersion(schemaUri);
            if ( !ActionSchemaHelper.isSupportedSchemaVersion(schemaVersion) ) {
                actionValidationHandler.onUnsupportedSchemaVersion(metadata, schemaVersion);
            }
        }
        
        final String getSchemaUri() {
            var matcher = schemaPattern.matcher(getActionText());
            String propertyValue = null;
            String commentValue = null;
            while ( matcher.find() ) {
                propertyValue = getValue("$schema", matcher.group("schemaPropertyValue"), propertyValue);
                commentValue = getValue("# yaml-language-server $schema", matcher.group("schemaCommentValue"), commentValue);
            }
            if ( StringUtils.isAllBlank(propertyValue, commentValue) ) {
                throw new IllegalStateException(getExceptionMessage("Either '$schema' property or '# yaml-language-server $schema' must be specified"));
            } else if ( StringUtils.isNoneBlank(propertyValue, commentValue) && !propertyValue.equals(commentValue) ) {
                throw new IllegalStateException(getExceptionMessage("If both '$schema' property and '# yaml-language-server $schema' are specified, the schema locations must be identical"));
            } else if ( StringUtils.isBlank(propertyValue) ) {
                return commentValue;
            } else {
                return propertyValue;
            }
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
            var msg = "Error loading action "+metadata.getName();
            if ( StringUtils.isNotBlank(detailMessage) ) { msg+=": "+detailMessage; }
            return msg;
        }
    }
    
    @Data @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ActionSource {
        private final Supplier<InputStream> inputStreamSupplier;
        private final ActionMetadata metadata;
        
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
            return new ActionSource(()->createSourceInputStream(source, true), ActionMetadata.create(true));
        }
        
        private static final ActionSource imported(String type) {
            return new ActionSource(customActionsInputStreamSupplier(type), ActionMetadata.create(true));
        }
        
        private static final ActionSource builtin(String type) {
            return new ActionSource(builtinActionsInputStreamSupplier(type), ActionMetadata.create(false));
        }
        
        private static final ActionSource common(String type) {
            return new ActionSource(commonActionsInputStreamSupplier(), ActionMetadata.create(false));
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
    
    @Data @Builder(toBuilder = true)
    public static final class ActionValidationHandler {
        public static final ActionValidationHandler PROMPT = ActionValidationHandler.builder()
                .onSignatureStatusDefault(ActionInvalidSignatureHandler.prompt)
                .onUnsupportedSchemaVersion(ActionInvalidSchemaVersionHandler.prompt)
                .build();
        public static final ActionValidationHandler WARN = ActionValidationHandler.builder()
                .onSignatureStatusDefault(ActionInvalidSignatureHandler.warn)
                .onUnsupportedSchemaVersion(ActionInvalidSchemaVersionHandler.warn)
                .build();
        public static final ActionValidationHandler IGNORE = ActionValidationHandler.builder()
                .onSignatureStatusDefault(ActionInvalidSignatureHandler.ignore)
                .onUnsupportedSchemaVersion(ActionInvalidSchemaVersionHandler.ignore)
                .build();
        @Singular private final List<String> extraPublicKeys;
        @Singular private final Map<SignatureStatus, BiConsumer<ActionMetadata, SignedTextDescriptor>> onSignatureStatuses;
        @Builder.Default private final BiConsumer<ActionMetadata, SignedTextDescriptor> onSignatureStatusDefault = ActionInvalidSignatureHandler.prompt;
        @Builder.Default private final BiConsumer<ActionMetadata, String> onUnsupportedSchemaVersion = ActionInvalidSchemaVersionHandler.prompt;
        
        public final SignatureValidator getSignatureValidator(ActionMetadata metadata) {
            return new SignatureValidator(d->handleInvalidSignature(metadata, d), extraPublicKeys.toArray(String[]::new));
        }
        public final void onUnsupportedSchemaVersion(ActionMetadata metadata, String schemaVersion) {
            this.onUnsupportedSchemaVersion.accept(metadata, schemaVersion);
        }
        private final void handleInvalidSignature(ActionMetadata metadata, SignedTextDescriptor signedTextDescriptor) {
            var consumer = onSignatureStatuses.get(signedTextDescriptor.getSignatureStatus());
            if ( consumer==null ) { consumer = onSignatureStatusDefault; }
            consumer.accept(metadata, signedTextDescriptor);
        }
        
        @RequiredArgsConstructor
        public static enum ActionInvalidSignatureHandler implements BiConsumer<ActionMetadata, SignedTextDescriptor> {
            ignore((p,d)->{}),
            warn((p,d)->_warn(signatureFailureMessage(p,d))),
            fail((p,d)->_throw(signatureFailureMessage(p,d))),
            prompt((p,d)->_prompt(signatureFailureMessage(p,d)));
            private final BiConsumer<ActionMetadata, SignedTextDescriptor> onInvalidSignature;
            
            @Override
            public void accept(ActionMetadata metadata, SignedTextDescriptor descriptor) {
                onInvalidSignature.accept(metadata, descriptor);   
            }
            
            private static final String signatureFailureMessage(ActionMetadata metadata, SignedTextDescriptor descriptor) {
                return getSignatureStatusMessage(metadata, descriptor.getSignatureStatus());
            }
        }
        
        @RequiredArgsConstructor
        public static enum ActionInvalidSchemaVersionHandler implements BiConsumer<ActionMetadata, String> {
            ignore((p,v)->{}),
            warn((p,v)->_warn(unsupportedSchemaMessage(p,v))),
            fail((p,v)->_throw(unsupportedSchemaMessage(p,v))),
            prompt((p,v)->_prompt(unsupportedSchemaMessage(p,v)));
            private final BiConsumer<ActionMetadata, String> onInvalidSchemaVersion;
            
            @Override
            public void accept(ActionMetadata metadata, String schemaVersion) {
                onInvalidSchemaVersion.accept(metadata, schemaVersion);   
            }
            
            public static final String unsupportedSchemaMessage(ActionMetadata metadata, String unsupportedVersion) {
                return String.format("Action "+metadata.getName()+" uses unsupported schema version %s and may fail.", unsupportedVersion);
            }
        }
        
        private static final void _warn(String msg) { LOG.warn("WARN: "+msg); }
        private static final void _throw(String msg) { throw new IllegalStateException(msg); }
        private static final void _prompt(String msg) {
            if ( System.console()==null ) {
                _throw(msg);
            } else if (!"Y".equalsIgnoreCase(System.console().readLine(String.format("WARN: %s\n  Do you want to continue? (Y/N) ", msg))) ) {
                throw new AbortedByUserException("Aborting: operation aborted by user");
            }
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
