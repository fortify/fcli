package com.fortify.cli.aviator.project.cli.cmd;

import com.fortify.aviator.project.Project;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "list")
public class AviatorProjectListCommand extends AbstractRunnableCommand implements Callable<Integer> {
    @Getter  @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private CommandHelperMixin commandHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectListCommand.class);

    @Override
    public Integer call() throws Exception {
        try {
            initMixins();
            var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
            try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
                String message = String.format("%s;%s", sessionDescriptor.getTenant(), ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
                Path keyFile = Path.of(sessionDescriptor.getPrivateKeyFile());
                String signature = SignatureHelper.signer(keyFile, (char[]) null).sign(message, StandardCharsets.UTF_8);
                List<Project> projects = client.listProjects(sessionDescriptor.getTenant(), signature, message);
                if (projects.isEmpty()) {
                    System.out.println("No projects found.");
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ArrayNode projectsArray = objectMapper.createArrayNode();

                    for (Project project : projects) {
                        ObjectNode projectNode = objectMapper.createObjectNode();

                        projectNode.put("id", project.getId());
                        projectNode.put("name", project.getName());
                        projectNode.put("createdat", project.hasCreatedAt());
                        projectNode.put("isdeleted", project.getIsDeleted());

                        projectsArray.add(projectNode);
                    }

                    outputHelper.write(projectsArray);
                }
            }
            return 0;
        } catch (NumberFormatException e) {
            LOG.error("Error parsing port number: {}", e.getMessage());
            return 1;
        }
        catch (Exception e) {
            LOG.error(String.valueOf(e.getCause()));
            LOG.error("Error listing projects: {}", e.getMessage());
            return 1;
        }
    }
}