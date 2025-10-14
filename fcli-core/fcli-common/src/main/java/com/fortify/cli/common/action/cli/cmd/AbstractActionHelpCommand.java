/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.action.cli.cmd;

import java.nio.charset.StandardCharsets;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.cli.mixin.ActionResolverMixin;
import com.fortify.cli.common.action.helper.ActionDescriptionRenderer;
import com.fortify.cli.common.action.helper.ActionDescriptionRenderer.ActionDescriptionRendererType;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.runner.processor.ActionCliOptionsProcessor.ActionOptionHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeyDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureMetadata;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.JsonHelper;

import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Unmatched;

public abstract class AbstractActionHelpCommand extends AbstractRunnableCommand {
    private static final ActionDescriptionRenderer descriptionRenderer = 
            ActionDescriptionRenderer.create(ActionDescriptionRendererType.PLAINTEXT);
    @Mixin private ActionResolverMixin.RequiredParameter actionResolver;
    @Unmatched private String[] actionArgs; // We explicitly ignore any unknown CLI args, to allow for 
                                            // users to simply switch between run and help commands.
    
    @Override
    public final Integer call() {
        initialize();
        var action = actionResolver.loadAction(getType(), ActionValidationHandler.WARN);
        System.out.println(getActionHelp(action));
        return 0;
    }
    
    private final String getActionHelp(Action action) {
        var metadata = action.getMetadata();
        var usage = action.getUsage();
        var result = String.format(
            "\nAction: %s\n"+
            "\n%s\n"+
            "\n%s\n"+
            "Metadata:\n%s"+
            "\nSynopsis: %s run %s [options]\n",
            metadata.getName(), usage.getHeader(), descriptionRenderer.render(action.getUsage().getDescription()), getMetadata(action), getActionCmd(), metadata.getName());
        var supportedOptionsTable = ActionOptionHelper.getSupportedOptionsTable(action);
        if ( StringUtils.isNotBlank(supportedOptionsTable) ) {
            result += String.format("\nAction options:\n%s", supportedOptionsTable);
        }
        return result;
    }

    @SneakyThrows
    private String getClasspathResourceAsString(String path) {
        var is = this.getClass().getResourceAsStream(path);
        if ( is==null ) {
            throw new FcliBugException(String.format("Class path resource %s not found", path));
        }
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }

    private final String getMetadata(Action action) {
        var metadata = action.getMetadata();
        var signatureDescriptor = metadata.getSignatureDescriptor();
        var signatureMetadata = signatureDescriptor==null ? null : signatureDescriptor.getMetadata();
        if ( signatureMetadata==null ) { signatureMetadata = SignatureMetadata.builder().build(); }
        var extraSignatureInfo = signatureMetadata.getExtraInfo();
        var publicKeyDescriptor = metadata.getPublicKeyDescriptor();
        if ( publicKeyDescriptor==null ) { publicKeyDescriptor = PublicKeyDescriptor.builder().build(); }
        var signatureStatus = metadata.getSignatureStatus();
        var data = JsonHelper.getObjectMapper().createObjectNode();
        data.put("Origin", metadata.isCustom()?"CUSTOM":"FCLI");
        data.put("Signature status", signatureStatus.toString());
        data.put("Author", StringUtils.defaultIfBlank(action.getAuthor(), "N/A"));
        if ( signatureStatus!=SignatureStatus.UNSIGNED ) {
            data.put("Signed by", StringUtils.defaultIfBlank(signatureMetadata.getSigner(), "N/A"));
        }
        switch (signatureStatus) {
        case NO_PUBLIC_KEY: 
            data.put("Required public key", StringUtils.defaultIfBlank(signatureDescriptor.getPublicKeyFingerprint(), "N/A"));
            break;
        case VALID:
            data.put("Certified by", StringUtils.defaultIfBlank(publicKeyDescriptor.getName(), 
                    StringUtils.defaultIfBlank(publicKeyDescriptor.getFingerprint(), "N/A")));
            break;
        default: break;
        }
        if ( extraSignatureInfo!=null && extraSignatureInfo.size()>0 ) {
            data.set("Extra signature info", extraSignatureInfo);
        }
        return toString(data, "  ");  
    }
    
    private static final String toString(ObjectNode data, String indent) {
        var sb = new StringBuffer();
        Iterable<String> iterable = () -> data.fieldNames();
        var nameLength = StreamSupport.stream(iterable.spliterator(), false)
                .mapToInt(String::length)
                .max().getAsInt();
        var fmt = indent+"%-"+(nameLength+1)+"s %s\n";
        data.fields().forEachRemaining(e->sb.append(String.format(fmt, e.getKey()+":", toValue(e.getValue(), indent))));
        return sb.toString();
    }
    
    private static final String toValue(JsonNode value, String originalIndent) {
        if ( value instanceof ObjectNode ) {
            return "\n"+toString((ObjectNode)value, originalIndent+"  ");
        } else {
            return value.asText();
        }
    }
    
    protected abstract String getType();
    protected abstract String getActionCmd();
}
