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

import java.util.concurrent.Callable;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.runner.ActionRunnerCommand;
import com.fortify.cli.common.cli.util.FortifyCLIDefaultValueProvider;
import com.fortify.cli.common.crypto.helper.SignatureHelper.PublicKeyDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureMetadata;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignatureStatus;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;

import lombok.Builder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

@Builder
public class ActionCommandLineFactory {
    /** Action run command for which to generate a CommandLine instance */
    private final String runCmd;
    /** Action to run, provided through builder method */
    private final Action action;
    /** ActionParameterHelper instance, configured through builder method */
    private final ActionParameterHelper actionParameterHelper;
    /** ActionRunnerCommand instance, configured through builder method */
    private final ActionRunnerCommand actionRunnerCommand;

    public final CommandLine createCommandLine() {
        CommandLine cl = new CommandLine(createCommandSpec());
        cl.setDefaultValueProvider(FortifyCLIDefaultValueProvider.getInstance());
        return cl;
    }
    
    private final CommandSpec createCommandSpec() {
        var fullCmdName = String.format("%s %s [run-options]", runCmd, action.getMetadata().getName());
        CommandSpec actionCmd = CommandSpec.forAnnotatedObject(actionRunnerCommand)
                .name(fullCmdName)
                .resourceBundleBaseName("com.fortify.cli.common.i18n.ActionMessages");
        addUsage(actionCmd);
        actionParameterHelper.addOptions(actionCmd);
        return actionCmd;
    }

    private final void addUsage(CommandSpec actionCmd) {
        actionCmd.usageMessage().header(action.getUsage().getHeader());
        actionCmd.usageMessage().description(getUsageDescription());
    }

    private final String getUsageDescription() {
        return String.format("%s\n\n%s\n\n%s",
                cleanWhitespace(action.getUsage().getDescription()),
                "[run-options] in the synopsis above refers to options documented "+
                "on the `"+runCmd+"` command, for example specifying where the "+
                "action is loaded from, and signature verification options.",
                getMetadataDescription().trim());
    }

    private Object cleanWhitespace(String s) {
        // TODO Improve formatting? Or just have yaml files provide string?
        return s.replaceAll("([^\\n])\\n([^\\n])", "$1 $2").replaceAll("[ ]+", " ").trim();
    }

    private final String getMetadataDescription() {
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
        return "Metadata:\n"+toString(data, "  ");  
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

    @Command
    private static final class DummyCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            return 0;
        }
    }
}
