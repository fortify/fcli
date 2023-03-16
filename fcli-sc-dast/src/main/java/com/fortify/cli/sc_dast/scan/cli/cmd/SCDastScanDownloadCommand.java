package com.fortify.cli.sc_dast.scan.cli.cmd;

import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.sc_dast.output.cli.cmd.AbstractSCDastOutputCommand;
import com.fortify.cli.sc_dast.output.cli.mixin.SCDastOutputHelperMixins;
import com.fortify.cli.sc_dast.scan.cli.mixin.SCDastScanResolverMixin;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SCDastOutputHelperMixins.Download.CMD_NAME)
public class SCDastScanDownloadCommand extends AbstractSCDastOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SCDastOutputHelperMixins.Download outputHelper;
    @Mixin private SCDastScanResolverMixin.PositionalParameter scanResolver;
    @Mixin private CommonOptionMixins.OptionalDestinationFile optionalDestination;
    @Option(names = {"-t", "--type"}, required=true, converter = DownloadTypeConverter.class, completionCandidates = DownloadTypeIterable.class)
    @Getter private DownloadType type;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SCDastScanDescriptor descriptor = scanResolver.getScanDescriptor(unirest);
        String downloadPath = optionalDestination.getDestination();
        if ( StringUtils.isBlank(downloadPath) ) {
            String identifier = StringUtils.isBlank(descriptor.getName()) 
                    ? String.format("scan-%s", descriptor.getId())
                    : descriptor.getName().replaceAll("\\s", "-");
            downloadPath = String.format("scdast-%s-%s.%s", identifier, type.formattedName(), type.getExtension());
        }
        unirest.get("/api/v2/scans/{id}/{endpoint}")
            .routeParam("id", scanResolver.getScanId())
            .routeParam("endpoint", type.getEndpoint())
            //.downloadMonitor(new SSCProgressMonitor("Download"))
            .asFile(downloadPath, StandardCopyOption.REPLACE_EXISTING)
            .getBody();
        return descriptor.asJsonNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return type.name().toUpperCase()+"_DOWNLOADED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @RequiredArgsConstructor @Getter
    public static enum DownloadType {
        fpr("download-fpr", "fpr"), 
        logs("download-logs", "zip"), 
        results("download-results", "scan"), 
        settings("download-scan-settings-xml", "xml"), 
        site_tree("download-site-tree", "csv");
        
        private final String endpoint;
        private final String extension;
        
        public String formattedName() {
            return name().replace('_', '-');
        }
        
        public static final String[] formattedNames() {
            return Stream.of(DownloadType.values())
                    .map(DownloadType::formattedName)
                    .toArray(String[]::new);
        }
        
        public static final DownloadType valueOfFormattedName(String s) {
            return DownloadType.valueOf(s.replace('-', '_'));
        }
    }
    
    public static final class DownloadTypeConverter implements ITypeConverter<DownloadType> {
        @Override
        public DownloadType convert(String value) throws Exception {
            return DownloadType.valueOfFormattedName(value);
        }
    }
    
    public static final class DownloadTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public DownloadTypeIterable() { 
            super(Arrays.asList(DownloadType.formattedNames())); 
        }
    }
}
