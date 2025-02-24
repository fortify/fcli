package com.fortify.cli.aviator.project.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.project.Project;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Mixin;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "get")
public class AviatorProjectGetCommand extends AbstractRunnableCommand implements Callable<Integer> {

    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    @Parameters(index = "0", description = "Project ID") private String projectId;

    @Override
    public Integer call() throws Exception {
        initMixins();
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String message = String.format("%s;%s;%s",sessionDescriptor.getTenant(), projectId, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
            Path keyFile = Path.of(sessionDescriptor.getPrivateKeyFile());
            String signature = SignatureHelper.signer(keyFile, (char[]) null).sign(message, StandardCharsets.UTF_8);
            Project project = client.getProject(projectId,signature,message,sessionDescriptor.getTenant());
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode getProjectNode = objectMapper.createObjectNode();
            getProjectNode.put("id", project.getId());
            getProjectNode.put("name",project.getName());
            getProjectNode.put("updatedAt",project.hasUpdatedAt());

            outputHelper.write(getProjectNode);
        }catch (Exception e){
            System.err.println("Error Getting project: " + e.getMessage());
            return 1;
        }
        return 0;
    }
}
