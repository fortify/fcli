/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.action.cli.cmd;

import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.cli.mixin.ActionResolverMixin;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.runner.ActionParameterHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeyDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureMetadata;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;

import picocli.CommandLine.Mixin;
import picocli.CommandLine.Unmatched;

public abstract class AbstractActionHelpCommand extends AbstractRunnableCommand {
    @Mixin private ActionResolverMixin.RequiredParameter actionResolver;
    @Unmatched private String[] actionArgs; // We explicitly ignore any unknown CLI args, to allow for 
                                            // users to simply switch between run and help commands.
    
    @Override
    public final Integer call() {
        initMixins();
        var action = actionResolver.loadAction(getType(), ActionValidationHandler.WARN);
        System.out.println(getActionHelp(action));
        return 0;
    }
    
    private final String getActionHelp(Action action) {
        var metadata = action.getMetadata();
        var usage = action.getUsage();
        return String.format(
            "\nAction: %s\n"+
            "\n%s\n"+
            "\n%s\n"+
            "Metadata:\n"+
            "%s"+
            "\nAction options:\n"+
            "%s",
            metadata.getName(), usage.getHeader(), usage.getDescription(), getMetadata(action), ActionParameterHelper.getSupportedOptionsTable(action));
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
        data.put("Author", StringUtils.ifBlank(action.getAuthor(), "N/A"));
        if ( signatureStatus!=SignatureStatus.UNSIGNED ) {
            data.put("Signed by", StringUtils.ifBlank(signatureMetadata.getSigner(), "N/A"));
        }
        switch (signatureStatus) {
        case NO_PUBLIC_KEY: 
            data.put("Required public key", StringUtils.ifBlank(signatureDescriptor.getPublicKeyFingerprint(), "N/A"));
            break;
        case VALID:
            data.put("Certified by", StringUtils.ifBlank(publicKeyDescriptor.getName(), 
                    StringUtils.ifBlank(publicKeyDescriptor.getFingerprint(), "N/A")));
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
}
