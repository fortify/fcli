package com.fortify.cli.aviator.project.cli.cmd;

import com.fortify.aviator.project.Project;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.crypto.helper.SignatureHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@Command(name = "create")
public class AviatorProjectCreateCommand extends AbstractRunnableCommand implements Callable<Integer>{

    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    @Option(names = {"-n", "--name"}, required = true) private String projectName;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectCreateCommand.class);

    @Override
    public Integer call() throws Exception {
        initMixins();
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {

            String message = String.format("%s;%s;%s", sessionDescriptor.getTenant(), projectName, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

            String signature = SignatureHelper.signer(sessionDescriptor.getPrivateKeyFile(), (char[]) null).sign(message, StandardCharsets.UTF_8);

            Project createdProject = client.createProject(projectName, sessionDescriptor.getTenant(), signature, message);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode projectNode = objectMapper.createObjectNode();
            projectNode.put("id", createdProject.getId());
            projectNode.put("name", createdProject.getName());
            projectNode.put("createdAt", createdProject.hasCreatedAt());
            projectNode.put("isDeleted", createdProject.getIsDeleted());
            outputHelper.write(projectNode);

        } catch (Exception e) {
            LOG.error("Error creating project: {}", e.getMessage());
            return 1;
        }
        return 0;
    }
}