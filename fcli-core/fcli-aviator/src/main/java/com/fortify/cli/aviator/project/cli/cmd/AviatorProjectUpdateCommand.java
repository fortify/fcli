package com.fortify.cli.aviator.project.cli.cmd;

import com.fortify.aviator.project.Project;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.core.IssueAuditor;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@Command(name = "update")
public class AviatorProjectUpdateCommand extends AbstractRunnableCommand implements Callable<Integer> {

    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    @Parameters(index = "0", description = "Project ID") private String projectId;
    @Option(names = {"-n", "--name"}, required = true) private String newName;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectUpdateCommand.class);

    @Override
    public Integer call() throws Exception {
        initMixins();
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String message = String.format("%s;%s;%s;%s", sessionDescriptor.getTenant(), projectId, newName, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
            Path keyFile = Path.of(sessionDescriptor.getPrivateKeyFile());
            String signature = SignatureHelper.signer(keyFile, (char[]) null).sign(message, StandardCharsets.UTF_8);
            Project updatedProject = client.updateProject(projectId, newName, signature, message, sessionDescriptor.getTenant());            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode updateProjectNode = objectMapper.createObjectNode();
            updateProjectNode.put("id", updatedProject.getId());
            updateProjectNode.put("name",updatedProject.getName());
            updateProjectNode.put("updatedAt",updatedProject.hasUpdatedAt());

            outputHelper.write(updateProjectNode);
        } catch (Exception e) {
            LOG.error("Error updating project: {}", e.getMessage());
            return 1;
        }
        return 0;
    }
}